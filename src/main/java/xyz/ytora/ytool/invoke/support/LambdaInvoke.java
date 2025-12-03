package xyz.ytora.ytool.invoke.support;

import xyz.ytora.ytool.classcache.ClassCache;
import xyz.ytora.ytool.classcache.classmeta.FieldMetadata;
import xyz.ytora.ytool.classcache.classmeta.MethodMetadata;
import xyz.ytora.ytool.invoke.Invoke;

import java.lang.invoke.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * created by yangtong on 2025/4/15 13:15:41
 * <br/>
 * 基于LambdaMetafactory实现的调用
 */

public class LambdaInvoke implements Invoke {

    private static final ConcurrentHashMap<String, Function<?, ?>> getterCache = new ConcurrentHashMap<>();
    private static final Map<String, BiConsumer<?, ?>> setterCache = new ConcurrentHashMap<>();
    private static final Map<String, MethodHandle> methodHandleCache = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Object getFieldValue(T target, String fieldName) {
        String key = target.getClass().getName() + "#" + fieldName;
        Function<T, Object> getter = (Function<T, Object>) getterCache.computeIfAbsent(key, k -> {
            try {
                FieldMetadata fieldMeta = ClassCache.getField(target.getClass(), fieldName);
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle getterHandle = lookup.unreflectGetter(fieldMeta.getSourceField());

                CallSite site = LambdaMetafactory.metafactory(
                        lookup,
                        "apply",
                        MethodType.methodType(Function.class),
                        MethodType.methodType(Object.class, Object.class),
                        getterHandle,
                        MethodType.methodType(fieldMeta.getSourceField().getType(), target.getClass())
                );

                return (Function<T, Object>) site.getTarget().invoke();
            } catch (Throwable e) {
                throw new RuntimeException("创建 getter lambda 失败: " + fieldName, e);
            }
        });

        return getter.apply(target);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void setFieldValue(T target, String fieldName, Object value) {
        String key = target.getClass().getName() + "#" + fieldName;
        BiConsumer<T, Object> setter = (BiConsumer<T, Object>) setterCache.computeIfAbsent(key, k -> {
            try {
                FieldMetadata fieldMeta = ClassCache.getField(target.getClass(), fieldName);
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle setterHandle = lookup.unreflectSetter(fieldMeta.getSourceField());

                CallSite site = LambdaMetafactory.metafactory(
                        lookup,
                        "accept",
                        MethodType.methodType(BiConsumer.class),
                        MethodType.methodType(void.class, Object.class, Object.class),
                        setterHandle,
                        MethodType.methodType(void.class, target.getClass(), fieldMeta.getSourceField().getType())
                );

                return (BiConsumer<T, Object>) site.getTarget().invoke();
            } catch (Throwable e) {
                throw new RuntimeException("创建 setter lambda 失败: " + fieldName, e);
            }
        });

        setter.accept(target, value);
    }

    @Override
    public <T> Object invokeMethod(T target, String methodName, Class<?>[] paramTypes, Object... args) {
        String key = target.getClass().getName() + "#" + methodName + "#" + String.join(",", getTypeNames(paramTypes));

        try {
            MethodHandle handle = methodHandleCache.computeIfAbsent(key, k -> {
                try {
                    MethodMetadata methodMeta = ClassCache.getMethod(target.getClass(), methodName, paramTypes);
                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    return lookup.unreflect(methodMeta.getOriginMethod());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("创建方法句柄失败: " + methodName, e);
                }
            });

            return handle.bindTo(target).invokeWithArguments(args);
        } catch (Throwable e) {
            throw new RuntimeException("方法调用失败: " + methodName, e);
        }
    }

    private static String[] getTypeNames(Class<?>[] types) {
        if (types == null || types.length == 0) return new String[0];
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].getName();
        }
        return names;
    }
}
