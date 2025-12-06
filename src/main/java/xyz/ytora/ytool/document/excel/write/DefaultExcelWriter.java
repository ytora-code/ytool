package xyz.ytora.ytool.document.excel.write;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import xyz.ytora.ytool.classcache.classmeta.ClassMetadata;
import xyz.ytora.ytool.classcache.classmeta.FieldMetadata;
import xyz.ytora.ytool.document.DocException;
import xyz.ytora.ytool.document.excel.*;
import xyz.ytora.ytool.invoke.Reflects;
import xyz.ytora.ytool.str.Strs;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;

/**
 * created by yang tong on 2025/5/30 11:36:26
 * <br/>
 * Excel写入的默认实现类(写入表格)
 */
public class DefaultExcelWriter<T> implements IExcelWriter<T> {

//    private final Map<String, CellStyle> styles = new HashMap<>();

    private final ExcelConfig<T> excelConfig;

    public DefaultExcelWriter(ExcelConfig<T> excelConfig) {
        this.excelConfig = excelConfig;
    }

    @Override
    public void doWrite(List<T> data) {
        OutputStream out = excelConfig.getOut();
        if (out == null) {
            throw new DocException("必须指定out");
        }
        //根据是否显示导出信息，推断表头所在行
        int headerIndex = excelConfig.getShowExpertInfo() ? 1 : 0;

        //获取类信息
        ClassMetadata<T> classMetadata = excelConfig.getClassMetadata();
        Excel excelAnno = classMetadata.getAnnotation(Excel.class);
        //Excel文件版本
        ExcelVersion version = excelAnno == null ? excelConfig.getVersion() : excelAnno.version();
        //写入的sheet名称
        String sheetName = excelAnno == null ? classMetadata.getSourceClass().getSimpleName() : excelAnno.value();

        //获取表头信息
        List<FieldMetadata> fieldMetadataList = classMetadata.getFields(f -> f.hasAnnotation(Excel.class)).stream()
                .filter(f -> !f.getAnnotation(Excel.class).ignore())
                .sorted(Comparator.comparingInt(f -> f.getAnnotation(Excel.class).index()))
                .toList();

        try (Workbook workbook = WorkbookGen.gen(version)) {
            Sheet sheet = workbook.createSheet(sheetName);

            //导出信息
            if (excelConfig.getShowExpertInfo()) {
                Row infoRow = sheet.createRow(0);
                infoRow.setHeightInPoints(25);

                //合并前几列展示信息
                Cell infoCell = infoRow.createCell(0);
                infoCell.setCellValue(excelConfig.getExpertInfo());
                infoCell.setCellStyle(getInfoStyle(workbook));
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, fieldMetadataList.size() - 1));
            }

            //创建表头（默认第二行）
            Row headerRow = sheet.createRow(headerIndex);
            //设置行高
            headerRow.setHeightInPoints(30);

            //冻结前两行
            sheet.createFreezePane(0, headerIndex + 1);

            CellStyle headerStyle = getHeaderStyle(workbook);
            //遍历并写入表头
            for (int i = 0; i < fieldMetadataList.size(); i++) {
                FieldMetadata fm = fieldMetadataList.get(i);
                Excel excel = fm.getAnnotation(Excel.class);
                String excelValue = excel.value();
                String headerName = Strs.isEmpty(excelValue) ? fm.getName() : excel.value();

                Cell cell = headerRow.createCell(i);
                //表头列的内容
                ExcelCell headerCell = new ExcelCell().setContent(headerName).setWorkbook(workbook);
                //表头列的样式
                headerCell.setCellStyle(headerStyle);
                //列宽
                sheet.setColumnWidth(i, excel.width() * 256);
                Cells.writeToCell(cell, headerCell);
            }

            CellStyle bodyStyle = getBodyStyle(workbook);
            //遍历并写入表体
            for (int i = 0; i < data.size(); i++) {
                T rowData = data.get(i);
                Row dataRow = sheet.createRow(i + headerIndex + 1);
                dataRow.setHeightInPoints(20);

                for (int j = 0; j < fieldMetadataList.size(); j++) {
                    FieldMetadata fm = fieldMetadataList.get(j);
                    //通过getter方法获取该字段的值
                    String getter = "get" + Strs.firstCapitalize(fm.getName());

                    try {
                        Object value = Reflects.invokeMethod(rowData, getter);
                        Cell cell = dataRow.createCell(j);
                        ExcelCell excelCell = new ExcelCell()
                                .setCell(cell)
                                .setContent(value)
                                .setWorkbook(workbook);
                        excelCell.setCellStyle(bodyStyle);
                        Cells.writeToCell(excelCell);
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new DocException("字段获取失败: " + fm.getName(), e);
                    }
                }
            }

            //写入输出流并flush
            workbook.write(out);
            if (out instanceof BufferedOutputStream) {
                out.flush();
            }
        } catch (Exception e) {
            throw new DocException("Excel写入失败: " + classMetadata.getSourceClass().getName(), e);
        }
    }

    /**
     * 导出信息行的样式
     */
    private CellStyle getInfoStyle(Workbook workbook) {
//        return styles.computeIfAbsent("info", key -> {
//
//        });
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        //边框
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setFont(font);

        return style;
    }

    /**
     * 表头的样式
     */
    private CellStyle getHeaderStyle(Workbook workbook) {
//        return styles.computeIfAbsent("header", key -> {
//
//        });
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        //背景颜色
        Color backgroundColor;
        if (workbook instanceof HSSFWorkbook) {
            backgroundColor = new HSSFColor(0x40, -1, new java.awt.Color(242, 242, 242));
        } else {
            backgroundColor = new XSSFColor(new java.awt.Color(242, 242, 242), null);
        }
        //style.setFillForegroundColor(LIGHT_TURQUOISE.index);
        style.setFillForegroundColor(backgroundColor);
        //填充模式
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true); // 自动换行

        //边框
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        //设置字体
        Font font = workbook.createFont();
        font.setFontName("黑体");
        //设置字体大小
        font.setFontHeightInPoints((short) 12);
        //加粗
        font.setBold(true);
        //斜体
        font.setItalic(false);
        style.setFont(font);
        return style;
    }

    /**
     * 表体的样式
     */
    private CellStyle getBodyStyle(Workbook workbook) {
//        return styles.computeIfAbsent("body", key -> {
//
//        });
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true); // 自动换行

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setFont(font);
        return style;
    }
}
