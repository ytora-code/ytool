package xyz.ytora.ytool.document.excel;

import org.apache.poi.ss.usermodel.*;
import xyz.ytora.ytool.str.Placeholder;

/**
 * 封装了poi中cell对象的一些常用信息
 */
public class ExcelCell {

    /**
     * 单元格对象
     */
    private Cell cell;
    /**
     * 单元格类型
     */
    private CellType cellType;
    /**
     * 单元格样式
     */
    private CellStyle cellStyle;

    /**
     * 单元格原始内容
     */
    private Object content;

    /**
     * 占位符前缀
     */
    private Placeholder prefix;

    /**
     * 占位符后缀
     */
    private Placeholder suffix;
    /**
     * 所属工作簿
     */
    private Workbook workbook;
    /**
     * 所属工作表
     */
    private Sheet sheet;

    public Cell getCell() {
        return cell;
    }

    public ExcelCell setCell(Cell cell) {
        this.cell = cell;
        return this;
    }

    public CellType getCellType() {
        return cellType;
    }

    public ExcelCell setCellType(CellType cellType) {
        this.cellType = cellType;
        return this;
    }

    public CellStyle getCellStyle() {
        return cellStyle;
    }

    public ExcelCell setCellStyle(CellStyle cellStyle) {
        this.cellStyle = cellStyle;
        return this;
    }

    public Object getContent() {
        return content;
    }

    public ExcelCell setContent(Object content) {
        this.content = content;
        return this;
    }

    public Placeholder getPrefix() {
        return prefix;
    }

    public ExcelCell setPrefix(Placeholder prefix) {
        this.prefix = prefix;
        return this;
    }

    public Placeholder getSuffix() {
        return suffix;
    }

    public ExcelCell setSuffix(Placeholder suffix) {
        this.suffix = suffix;
        return this;
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    public ExcelCell setWorkbook(Workbook workbook) {
        this.workbook = workbook;
        return this;
    }

    public Sheet getSheet() {
        return sheet;
    }

    public ExcelCell setSheet(Sheet sheet) {
        this.sheet = sheet;
        return this;
    }
}
