package xyz.ytora.ytool.document.excel.read;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import xyz.ytora.ytool.document.DocException;
import xyz.ytora.ytool.document.excel.*;
import xyz.ytora.ytool.invoke.Reflects;
import xyz.ytora.ytool.io.Ios;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * created by yangtong on 2025/5/29 20:36:54
 * <br/>
 * Excel读取默认实现类(只支持表格型，会重复读取每一行，并封装成一个对象，最后返回对象数字)
 */
public class DefaultExcelReader<T> implements IExcelReader<T> {

    /**
     * 表头名称和其列下标对应关系
     */
    Map<String, Integer> headerNameIndexMapper = new HashMap<>();

    private final ExcelConfig<T> excelConfig;

    public DefaultExcelReader(ExcelConfig<T> excelConfig) {
        this.excelConfig = excelConfig;
    }

    /**
     * 进行excel文件的读取操作，会自动关闭输入流excelIs
     * @param excelInput excel输入流
     * @return 解析后的对象数组
     */
    @Override
    public List<T> doRead(InputStream excelInput) {
        List<T> list;
        try (Workbook workbook = WorkbookGen.gen(excelInput)) {
            //获取sheet
            Sheet sheet = workbook.getSheetAt(excelConfig.getSheet());
            //是否启动表头索引
            if (excelConfig.getHeaderMappingEnabled()) {
                Row headerRow = sheet.getRow(excelConfig.getHeaderRowIndex());
                headerNameIndexMapper = parseHeader(headerRow);
            }

            //获取当前sheet的最后一行的下标，用于遍历行
            int lastRowNum = sheet.getLastRowNum();

            //从起始行开始，依次遍历每一行
            int startRow = excelConfig.getShowExpertInfo() ? excelConfig.getStartRow() + 1 : excelConfig.getStartRow();
            if (excelConfig.getHeaderMappingEnabled()) {
                //表头行应该被跳过，读取从表头下一行开始
                int headerRowIndex = excelConfig.getHeaderRowIndex();
                if (startRow <= headerRowIndex) {
                    startRow = headerRowIndex + 1;
                }
            }

            list = new ArrayList<>(Math.max(1, lastRowNum - startRow + 1));
            for (int i = startRow; i <= lastRowNum; i++) {
                Row currentRow = sheet.getRow(i);
                if (currentRow == null)
                    continue;
                //将每一行解析成对象
                T obj = parseRowSmart(currentRow, excelConfig);
                list.add(obj);
            }

        } catch (Exception e) {
            throw new DocException(e);
        } finally {
            Ios.close(excelInput);
        }
        return list;
    }

    /**
     * 将每一行解析成指定对象：三层匹配策略：index -> headerName -> 字段在所属类中的定义顺序
     * @param row excel行
     * @param excelConfig 配置信息
     * @return targetClazz对象
     */
    private T parseRowSmart(Row row, ExcelConfig<T> excelConfig) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<T> targetClazz = excelConfig.getTargetClazz();
        T targetObj = Reflects.newInstance(targetClazz);

        List<ExcelAnnoHandler> annoFields = excelConfig.getAnnoFields();
        boolean headerEnabled = excelConfig.getHeaderMappingEnabled();

        for (int i = 0; i < annoFields.size(); i++) {
            ExcelAnnoHandler handler = annoFields.get(i);

            //1.优先：index 显式列下标
            int orderIndex = handler.getExcel().index();
            if (orderIndex >= 0 && orderIndex < row.getLastCellNum()) {
                Cell cell = row.getCell(orderIndex);
                Object value = Cells.readFromCell(new ExcelCell().setCell(cell));
                handler.parse(targetObj, value);
                continue;
            }

            //2.次选：根据表头名匹配
            if (headerEnabled && headerNameIndexMapper != null) {
                String headerName = handler.getExcel().value().trim().toLowerCase();
                Integer headerIndex = headerNameIndexMapper.get(headerName);
                if (headerIndex != null && headerIndex < row.getLastCellNum()) {
                    Cell cell = row.getCell(headerIndex);
                    Object value = Cells.readFromCell(new ExcelCell().setCell(cell));
                    handler.parse(targetObj, value);
                    continue;
                }
            }

            //3.最后：按注解字段顺序
            if (i < row.getLastCellNum()) {
                Cell cell = row.getCell(i);
                Object value = Cells.readFromCell(new ExcelCell().setCell(cell));
                handler.parse(targetObj, value);
            }
        }

        return targetObj;
    }

    private Map<String, Integer> parseHeader(Row header) {
        headerNameIndexMapper = new HashMap<>();
        for (Cell headerCell : header) {
            Object headerValue = Cells.readFromCell(new ExcelCell().setCell(headerCell));
            if (headerValue != null) {
                String headerName = headerValue.toString().trim().toLowerCase();
                headerNameIndexMapper.put(headerName, headerCell.getColumnIndex());
            }
        }

        return headerNameIndexMapper;
    }
}
