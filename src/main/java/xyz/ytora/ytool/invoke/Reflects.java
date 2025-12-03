package xyz.ytora.ytool.invoke;

import xyz.ytora.ytool.classcache.ClassCache;
import xyz.ytora.ytool.classcache.classmeta.ConstructorMetadata;
import xyz.ytora.ytool.invoke.support.ReflectInvoke;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * created by yangtong on 2025/4/8 16:06:36
 * <br/>
 * 反射工具类
 */
public class Reflects {

    static Invoke invoke = new ReflectInvoke();

    //基本类型和包装类型映射
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            void.class, Void.class
    );

    /**
     * 根据class实例化对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> clazz, Object... args) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        if (Object.class.equals(clazz)) {
            return (T) new Object();
        }
        ConstructorMetadata<T> constructor = ClassCache.getConstructor(clazz, argsToClasses(args));
        return constructor.instance(args);
    }

    /**
     * 根据class构造合适的空对象
     */
    public static Object instantiateEmpty(Class<?> clazz) {

        // Optional -> Optional.empty()
        if (Optional.class.isAssignableFrom(clazz)) return Optional.empty();

        // 数组 -> 长度0
        if (clazz.isArray()) return Array.newInstance(clazz.getComponentType(), 0);

        // 集合接口/抽象类 -> 常用空实现
        if (Collection.class.isAssignableFrom(clazz)) return new ArrayList<>();

        // Map 接口/抽象类 -> 空 LinkedHashMap
        if (Map.class.isAssignableFrom(clazz)) return new LinkedHashMap<>();

        // 基元类型 -> 默认值
        if (clazz.isPrimitive()) {
            if (clazz == boolean.class) return false;
            if (clazz == char.class) return '\0';
            if (clazz == byte.class) return (byte) 0;
            if (clazz == short.class) return (short) 0;
            if (clazz == int.class) return 0;
            if (clazz == long.class) return 0L;
            if (clazz == float.class) return 0f;
            if (clazz == double.class) return 0d;
        }

        // String/包装类型/枚举 -> 返回 null（更符合语义）
        if (clazz == String.class || Number.class.isAssignableFrom(clazz) || clazz == Boolean.class || clazz.isEnum()) {
            return null;
        }

        // 其它 POJO：尝试无参构造
        try {
            return newInstance(clazz);
        } catch (Throwable e) {
            // log.error(e.getMessage(), e);
            //throw new IllegalArgumentException(Strs.format("为类型:[]构造空对象失败，必须提供无参构造方法"), e);
            return null;
        }
    }

    /**
     * 根据type构造合适的空对象（泛型版本）
     */
    public static Object instantiateEmpty(Type type) {
        if (type instanceof Class<?> c) {
            return instantiateEmpty(c);
        }
        if (type instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            if (raw instanceof Class<?> rc) {
                // Optional<T>
                if (Optional.class.isAssignableFrom(rc)) return Optional.empty();
                // Collection<T>
                if (Collection.class.isAssignableFrom(rc)) return new ArrayList<>();
                // Map<K,V>
                if (Map.class.isAssignableFrom(rc)) return new LinkedHashMap<>();
                return instantiateEmpty(rc);
            }
        }
        // 其它复杂 Type（通配、数组泛型等）——返回 null，由 mapper 兜底
        return null;
    }

    /**
     * 获取对象的字段
     */
    public static <T> Object getFieldValue(T target, String fieldName) throws InvocationTargetException, IllegalAccessException {
        return invoke.getFieldValue(target, fieldName);
    }

    /**
     * 获设置对象的字段值
     */
    public static <T> void setFieldValue(T target, String fieldName, Object value) throws InvocationTargetException, IllegalAccessException {
        invoke.setFieldValue(target, fieldName, value);
    }

    /**
     * 调用对象的方法
     */
    public static <T> Object invokeMethod(T target, String methodName, Class<?>[] paramTypes, Object... args) throws InvocationTargetException, IllegalAccessException {
        return invoke.invokeMethod(target, methodName, paramTypes, args);
    }

    /**
     * 调用对象的方法
     */
    public static <T> Object invokeMethod(T target, String methodName, Object... args) throws InvocationTargetException, IllegalAccessException {
        return invokeMethod(target, methodName, argsToClasses(args), args);
    }

    public static Class<?>[] argsToClasses(Object... args) {
        if (args == null) {
            return new Class<?>[]{Object.class};
        }
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                types[i] = Object.class;
            } else {
                types[i] = args[i].getClass();
            }
        }
        return types;
    }

    /**
     * 基本类型转包装类型
     */
    public static Class<?> primitiveToWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ? PRIMITIVE_TO_WRAPPER.get(clazz) : clazz;
    }

}
