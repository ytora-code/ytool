package xyz.ytora.ytool.json.config.convert;

import xyz.ytora.ytool.json.TypeRef;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 并发安全的转换器注册表：支持 exact(Type) 与 raw(Class) 两种注册与匹配
 */
public class ConverterRegistry {
    /**
     * 带泛型的类型转换器
     */
    private final Map<Type, JsonTypeConverter<?>> exact = new ConcurrentHashMap<>();
    /**
     * 普通类型转换器
     */
    private final Map<Class<?>, JsonTypeConverter<?>> raw = new ConcurrentHashMap<>();
    /**
     * 注册器是否冻结，如果冻结了，则不允许继续注册转换器
     */
    private final AtomicBoolean frozen = new AtomicBoolean(false);

    public <T> void register(TypeRef<T> ref, JsonTypeConverter<T> c) {
        register(ref.type(), c);
    }

    public <T> void register(Type type, JsonTypeConverter<T> c) {
        checkWritable();
        exact.put(canon(type), c);
    }

    public <T> void register(Class<T> rawType, JsonTypeConverter<T> c) {
        checkWritable();
        raw.put(rawType, c);
    }

    /** 冻结后不可再注册 */
    public void freeze() {
        frozen.set(true);
    }

    public boolean isFrozen() {
        return frozen.get();
    }

    public JsonTypeConverter<?> lookup(Type t) {
        if (t == null) return null;
        t = canon(t);
        JsonTypeConverter<?> c = exact.get(t);
        if (c != null) return c;

        if (t instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> rc) {
            c = lookupRaw(rc);
            return c;
        } else if (t instanceof Class<?> rc2) {
            c = lookupRaw(rc2);
            return c;
        }
        return null;
    }

    public JsonTypeConverter<?> lookup(Class<?> rawType) {
        return lookupRaw(rawType);
    }

    /**
     * 检查注册器是否冻结
     */
    private void checkWritable() {
        if (frozen.get())
            throw new IllegalStateException("ConverterRegistry has been frozen");
    }

    private JsonTypeConverter<?> lookupRaw(Class<?> rc) {
        // 直接命中
        JsonTypeConverter<?> c = raw.get(rc);
        if (c != null) return c;
        // 向上父类
        for (Class<?> cur = rc.getSuperclass(); cur != null; cur = cur.getSuperclass()) {
            c = raw.get(cur);
            if (c != null) return c;
        }
        // 接口
        for (Class<?> itf : rc.getInterfaces()) {
            c = raw.get(itf);
            if (c != null) return c;
        }
        return null;
    }

    private static Type canon(Type t) {
        return t;
    }
}
