package xyz.ytora.ytool.document.excel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * created by yangtong on 2025/4/4 下午4:46
 * <br/>
 * Excel注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface Excel {
    /**
     * excel列名称
     */
    String value() default "";

    /**
     * excel版本
     */
    ExcelVersion version() default ExcelVersion.V07;

    /**
     * 字段顺序
     */
    int index() default -1;

    /**
     * 列宽度
     */
    int width() default 20;

    /**
     * 日期、数字格式
     */
    String format() default "";

    /**
     * 字段处理器
     */
    Class<? extends IExcelFieldHandler>[] handlers() default {};

    /**
     * 是否跳过该字段
     */
    boolean ignore() default false;

    /**
     * EXCEL 文件名称
     */
    String fileName() default "";

    /**
     * 读取时：从第几行开始读
     */
    int startRow() default 1;

    /**
     * 读取时：从第几列开始读
     */
    int startCol() default 0;

    /**
     * 表头所在行
     */
    int headerRowIndex() default 1;
}