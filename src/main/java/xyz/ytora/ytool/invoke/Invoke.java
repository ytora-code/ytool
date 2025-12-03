package xyz.ytora.ytool.invoke;

import java.lang.reflect.InvocationTargetException;

/**
 * created by yangtong on 2025/4/15 13:10:44
 * <br/>
 * 调用规范，调用对象的方法，和读写对象的字段
 */
public interface Invoke {
    //获取对象的字段值
    <T> Object getFieldValue(T target, String fieldName) throws IllegalAccessException, InvocationTargetException;

    //设置对象的字段值
    <T> void setFieldValue(T target, String fieldName, Object value) throws InvocationTargetException, IllegalAccessException;

    //调用对象的方法
    <T> Object invokeMethod(T target, String methodName, Class<?>[] paramTypes, Object... args) throws InvocationTargetException, IllegalAccessException;
}
