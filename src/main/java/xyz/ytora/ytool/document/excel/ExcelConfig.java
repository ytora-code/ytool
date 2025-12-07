package xyz.ytora.ytool.document.excel;

import xyz.ytora.ytool.classcache.ClassCache;
import xyz.ytora.ytool.classcache.classmeta.ClassMetadata;
import xyz.ytora.ytool.date.Dates;
import xyz.ytora.ytool.document.excel.factory.DefaultExcelFieldHandlerFactory;
import xyz.ytora.ytool.document.excel.factory.ExcelFieldHandlerFactory;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * created by yangtong on 2025/5/29 20:46:47
 * <br/>
 * Excel读取或写入时的一些配置
 */
public class ExcelConfig<T> {
    /**
     * 读取exel第几个sheet的内容，默认第一个（下标0）
     */
    private Integer sheet = 0;

    /**
     * 表头所在行
     */
    private Integer headerRowIndex = 0;

    /**
     * 从第几行开始读取(包含表头)
     */
    private Integer startRow = 0;

    /**
     * 从第几列开始读取
     */
    private Integer startCol = 0;

    /**
     * 将excel读取成指定类型
     */
    private Class<T> targetClazz;

    /**
     * 类元信息
     */
    private ClassMetadata<T> classMetadata;

    /**
     * 目标对象中加了Excel注解的字段信息
     */
    private List<ExcelAnnoHandler> annoFields;

    /**
     * 表头和Excel注解的对照关系
     */
    private Map<String, ExcelAnnoHandler> annoHeaderFieldMapper;

    /**
     * 是否启用列名匹配模式
     */
    private Boolean headerMappingEnabled = true;

    /**
     * 默认操作的07版本的xlsx
     */
    private ExcelVersion version = ExcelVersion.V07;

    /**
     * 写入的数据条数
     */
    private Integer count;

    /**
     * 将excel数据写到指定输出流
     */
    private OutputStream out;

    /**
     * 是否在导出的excel里面显示导出信息
     */
    private Boolean showExpertInfo = true;

    /**
     * 导出用户
     */
    private String expertUser = "-";

    /**
     * 导出信息
     */
    private String expertInfo;

    public ExcelConfig(Class<T> targetClazz) {
        this(targetClazz, DefaultExcelFieldHandlerFactory.getInstance());
    }

    public ExcelConfig(Class<T> targetClazz, ExcelFieldHandlerFactory factory) {
        this.targetClazz = targetClazz;
        this.classMetadata = ClassCache.get(targetClazz);

        //获取目标对象中标准了Excel注解的字段
        annoFields = classMetadata.getFields().stream()
                .map(field -> new ExcelAnnoHandler(field, factory))
                .filter(ExcelAnnoHandler::hasExcel)
                .filter(i -> !i.getExcel().ignore())
                .toList();
        //解析出 表头和 EXCEL注解 的对照关系
        annoHeaderFieldMapper = annoFields.stream()
                .filter(h -> !h.getExcel().value().trim().isBlank())
                .collect(Collectors.toMap(h -> h.getExcel().value().trim().toLowerCase(), Function.identity()));
    }

    /**
     * 从指定模板加载
     */
    public ExcelConfig<T> form(InputStream in) {
        return this;
    }

    /**
     * 将excel写入到指定输出流
     */
    public ExcelConfig<T> to(OutputStream out) {
        if (!(out instanceof BufferedOutputStream)) {
            out = new BufferedOutputStream(out);
        }
        this.out = out;
        return this;
    }

    /**
     * 从指定行列下标（从0开始）开始读取
     */
    public ExcelConfig<T> setStart(Integer row, Integer col) {
        this.startRow = row;
        this.startCol = col;
        return this;
    }

    /**
     * 获取导出信息
     */
    public String getExpertInfo() {
        if (this.expertInfo != null) {
            return this.expertInfo;
        }
        return "导出人：" + expertUser + "    导出时间：" + Dates.now() + "    总条数：" + count;
    }

    public Integer getSheet() {
        return sheet;
    }

    public ExcelConfig<T> setSheet(Integer sheet) {
        this.sheet = sheet;
        return this;
    }

    public Integer getHeaderRowIndex() {
        return headerRowIndex;
    }

    public ExcelConfig<T> setHeaderRowIndex(Integer headerRowIndex) {
        this.headerRowIndex = headerRowIndex;
        return this;
    }

    public Integer getStartRow() {
        return startRow;
    }

    public ExcelConfig<T> setStartRow(Integer startRow) {
        this.startRow = startRow;
        return this;
    }

    public Integer getStartCol() {
        return startCol;
    }

    public ExcelConfig<T> setStartCol(Integer startCol) {
        this.startCol = startCol;
        return this;
    }

    public Class<T> getTargetClazz() {
        return targetClazz;
    }

    public ExcelConfig<T> setTargetClazz(Class<T> targetClazz) {
        this.targetClazz = targetClazz;
        return this;
    }

    public ClassMetadata<T> getClassMetadata() {
        return classMetadata;
    }

    public ExcelConfig<T> setClassMetadata(ClassMetadata<T> classMetadata) {
        this.classMetadata = classMetadata;
        return this;
    }

    public List<ExcelAnnoHandler> getAnnoFields() {
        return annoFields;
    }

    public ExcelConfig<T> setAnnoFields(List<ExcelAnnoHandler> annoFields) {
        this.annoFields = annoFields;
        return this;
    }

    public Map<String, ExcelAnnoHandler> getAnnoHeaderFieldMapper() {
        return annoHeaderFieldMapper;
    }

    public ExcelConfig<T> setAnnoHeaderFieldMapper(Map<String, ExcelAnnoHandler> annoHeaderFieldMapper) {
        this.annoHeaderFieldMapper = annoHeaderFieldMapper;
        return this;
    }

    public Boolean getHeaderMappingEnabled() {
        return headerMappingEnabled;
    }

    public ExcelConfig<T> setHeaderMappingEnabled(Boolean headerMappingEnabled) {
        this.headerMappingEnabled = headerMappingEnabled;
        return this;
    }

    public ExcelVersion getVersion() {
        return version;
    }

    public ExcelConfig<T> setVersion(ExcelVersion version) {
        this.version = version;
        return this;
    }

    public Integer getCount() {
        return count;
    }

    public ExcelConfig<T> setCount(Integer count) {
        this.count = count;
        return this;
    }

    public OutputStream getOut() {
        return out;
    }

    public ExcelConfig<T> setOut(OutputStream out) {
        this.out = out;
        return this;
    }

    public Boolean getShowExpertInfo() {
        return showExpertInfo;
    }

    public ExcelConfig<T> setShowExpertInfo(Boolean showExpertInfo) {
        this.showExpertInfo = showExpertInfo;
        return this;
    }

    public String getExpertUser() {
        return expertUser;
    }

    public ExcelConfig<T> setExpertUser(String expertUser) {
        this.expertUser = expertUser;
        return this;
    }

    public ExcelConfig<T> setExpertInfo(String expertInfo) {
        this.expertInfo = expertInfo;
        return this;
    }
}
