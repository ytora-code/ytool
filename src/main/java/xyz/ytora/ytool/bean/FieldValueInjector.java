package xyz.ytora.ytool.bean;

/**
 * created by yangtong on 2025/4/15 00:23:33
 * <br/>
 * 将bean对象的fieldName字段赋值为value
 */
@FunctionalInterface
public interface FieldValueInjector {
    void inject(Object bean, String fieldName, Object value);
}
