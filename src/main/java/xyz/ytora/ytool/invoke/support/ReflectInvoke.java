package xyz.ytora.ytool.invoke.support;

import xyz.ytora.ytool.classcache.ClassCache;
import xyz.ytora.ytool.classcache.classmeta.MethodMetadata;
import xyz.ytora.ytool.invoke.Invoke;
import xyz.ytora.ytool.invoke.InvokeException;
import xyz.ytora.ytool.invoke.Reflects;
import xyz.ytora.ytool.str.Strs;

import java.lang.reflect.InvocationTargetException;

/**
 * created by yangtong on 2025/4/15 13:13:57
 * <br/>
 * 基于反射实现的调用
 */
public class ReflectInvoke implements Invoke {
    @Override
    public <T> Object getFieldValue(T target, String fieldName) throws InvocationTargetException, IllegalAccessException {
        //通过getter方法获取字段的值
        String getterStr = "get" + Strs.firstCapitalize(fieldName);

        return invokeMethod(target, getterStr, new Class<?>[0]);
    }

    @Override
    public <T> void setFieldValue(T target, String fieldName, Object value) throws InvocationTargetException, IllegalAccessException {
        //获取setter方法名称
        String setterStr = "set" + Strs.firstCapitalize(fieldName);
        //获取方法的参数类型数组
        Class<?>[] types = Reflects.argsToClasses(value);

        invokeMethod(target, setterStr, types, value);
    }

    @Override
    public <T> Object invokeMethod(T target, String methodName, Class<?>[] paramTypes, Object... args) throws InvocationTargetException, IllegalAccessException {
        MethodMetadata method = ClassCache.getMethod(target.getClass(), methodName, paramTypes);
        if (!method.isPublic()) {
            throw new InvokeException("无访问权限");
        }
        return method.invoke(target, args);
    }
}
