package xyz.ytora.ytool.document.excel;

import java.lang.reflect.Field;

/**
 * EXCEL字段值 -> POJO字段 映射规则
 */
@FunctionalInterface
public interface IExcelFieldHandler<T, V, R> {
    /**
     * 处理方法
     * @param targetObj 目标对象
     * @param field 目标字段
     * @param value 从excel中读取出的原始数据
     * @return 经过处理后真正赋值给字段的值
     */
    R handler(T targetObj, Field field, V value);

}
