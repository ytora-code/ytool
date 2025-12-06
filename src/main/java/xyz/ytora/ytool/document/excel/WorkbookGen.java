package xyz.ytora.ytool.document.excel;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import xyz.ytora.ytool.document.DocException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * created by yangtong on 2025/5/29 23:05:50
 * <br/>
 * Workbook 对象生产工具类
 */
public class WorkbookGen {

    /**
     * 根据EXCEL文件流，判断应该使用何种Workbook对象
     */
    public static Workbook gen(InputStream excelIs) {
        try {
            //确保输入流为包装流
            if (!(excelIs instanceof BufferedInputStream)) {
                excelIs = new BufferedInputStream(excelIs);
            }
            ExcelVersion version = ExcelVersion.parseVersion(excelIs);
            return switch (version) {
                case V03 -> new HSSFWorkbook(excelIs);
                case V07 -> new XSSFWorkbook(excelIs);
                case V07_PLUS -> throw new DocException("不支持的类型:V07_PLUS");
            };
        } catch (IOException e) {
            throw new DocException(e);
        }
    }

    public static Workbook gen(ExcelVersion version) {
        return switch (version) {
            case V03 -> new HSSFWorkbook();
            case V07 -> new XSSFWorkbook();
            case V07_PLUS -> new SXSSFWorkbook();
        };
    }
}
