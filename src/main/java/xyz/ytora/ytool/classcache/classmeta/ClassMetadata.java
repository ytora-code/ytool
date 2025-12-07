package xyz.ytora.ytool.classcache.classmeta;

import xyz.ytora.ytool.anno.Index;
import xyz.ytora.ytool.classcache.ClassCacheException;
import xyz.ytora.ytool.str.Strs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 类元数据
 */
public class ClassMetadata<T> {

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();

    {
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(void.class, Void.class);
    }

    /**
     * 类对象
     */
    private final Class<T> sourceClass;
    /**
     * 全类名
     */
    private final String className;

    /**
     * 简单类名
     */
    private final String simpleName;
    /**
     * 类注解
     */
    private final Map<Class<? extends Annotation>, Annotation> classAnnotations;
    /**
     * 类字段
     */
    private final Map<String, FieldMetadata> fields = new LinkedHashMap<>();
    /**
     * 构造器方法
     */
    private final Map<String, ConstructorMetadata<T>> constructors = new LinkedHashMap<>();
    /**
     * 类方法
     */
    private final Map<String, MethodMetadata> methods = new LinkedHashMap<>();

    /**
     * 字段排序规则
     */
    Comparator<Field> fieldComparator = (f1, f2) -> {
        // 获取两个字段的 Order 值
        Index order1 = f1.getAnnotation(Index.class);
        Index order2 = f2.getAnnotation(Index.class);
        // 有注解返回注解值，没注解返回 Integer 最大值（排到最后）
        int v1 = (order1 != null) ? order1.value() : Integer.MAX_VALUE;
        int v2 = (order2 != null) ? order2.value() : Integer.MAX_VALUE;

        // 1. 先比数值
        int result = Integer.compare(v1, v2);

        // 2. 数值一样，比字段名字典序（防止顺序随机）
        if (result == 0) {
            return f1.getName().compareTo(f2.getName());
        }
        return result;
    };

    List<String> ignoreMethodList = List.of("toString", "equals", "canEqual", "hashCode", "clone");
    /**
     * 方法排序规则
     */
    Comparator<Method> methodComparator = (m1, m2) -> {
        // 获取两个字段的 Order 值
        Index order1 = m1.getAnnotation(Index.class);
        if (order1 == null) {
            String name = m1.getName();
            if (name.startsWith("get") || name.startsWith("set")) {
                name = Strs.firstLowercase(name.substring(3));
            } else if (name.startsWith("is")) {
                name = Strs.firstLowercase(name.substring(2));
            }
            try {
                Field f1 = m1.getDeclaringClass().getDeclaredField(name);
                order1 = f1.getAnnotation(Index.class);
            } catch (NoSuchFieldException ignored) {
            }
        }

        Index order2 = m2.getAnnotation(Index.class);
        if (order2 == null) {
            String name = m2.getName();
            if (name.startsWith("get") || name.startsWith("set")) {
                name = Strs.firstLowercase(name.substring(3));
            } else if (name.startsWith("is")) {
                name = Strs.firstLowercase(name.substring(2));
            }
            try {
                Field f2 = m2.getDeclaringClass().getDeclaredField(name);
                order2 = f2.getAnnotation(Index.class);
            } catch (NoSuchFieldException ignored) {
            }
        }

        // 有注解返回注解值，没注解返回 Integer 最大值（排到最后）
        int v1 = (order1 != null) ? order1.value() : Integer.MAX_VALUE;
        int v2 = (order2 != null) ? order2.value() : Integer.MAX_VALUE;

        // 1. 先比数值
        int result = Integer.compare(v1, v2);

        // 2. 数值一样，比字段名字典序（防止顺序随机）
        if (result == 0) {
            return m1.getName().compareTo(m2.getName());
        }
        return result;
    };

    public ClassMetadata(Class<T> sourceClass) {
        this.sourceClass = sourceClass;
        this.className = sourceClass.getName();
        this.simpleName = sourceClass.getSimpleName();
        this.classAnnotations = Arrays.stream(sourceClass.getAnnotations())
                .collect(Collectors.toMap(Annotation::annotationType, a -> a));
        collectFields(sourceClass);
        collectConstructor(sourceClass);
        collectMethods(sourceClass);
    }

    public String getClassName() {
        return className;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public Class<T> getSourceClass() {
        return sourceClass;
    }

    /**
     * 获取原始字段信息
     */
    public Map<String, FieldMetadata> getSourceFieldMap() {
        return fields;
    }

    /**
     * 获取当前class的指定字段
     */
    public FieldMetadata getField(String name) {
        FieldMetadata field = fields.get(name);
        if (field == null) {
            throw new ClassCacheException("从【" + sourceClass.getName() + "】类中未找到【" + name + "】字段");
        }
        return field;
    }

    /**
     * 获取当前class的全部字段
     */
    public List<FieldMetadata> getFields() {
        return getFields(null);
    }

    /**
     * 获取当前class的按指定规则过滤后的字段
     */
    public List<FieldMetadata> getFields(Predicate<FieldMetadata> filter) {
        if (filter == null) {
            filter = f -> true;
        }
        List<FieldMetadata> fieldMetadataList = new ArrayList<>();
        for (String fieldKey : fields.keySet()) {
            FieldMetadata fieldMetadata = fields.get(fieldKey);
            if (filter.test(fieldMetadata)) {
                fieldMetadataList.add(fieldMetadata);
            }
        }
        return fieldMetadataList;
    }

    public ConstructorMetadata<T> getConstructor(Class<?>... paramTypes) {
        String constructorKey = buildMethodKey(sourceClass.getName(), paramTypes);
        ConstructorMetadata<T> constructor = constructors.get(constructorKey);
        if (constructor == null) {
            throw new ClassCacheException("从【" + sourceClass.getName() + "】类中未找到签名为【" + constructorKey + "】的方法");
        }
        return constructor;
    }

    public MethodMetadata getMethod(String name, Class<?>... paramTypes) {
        String methodKey = buildMethodKey(name, paramTypes);
        MethodMetadata method = methods.get(methodKey);
        if (method == null) {
            throw new ClassCacheException("从【" + sourceClass.getName() + "】类中未找到签名为【" + methodKey + "】的方法");
        }
        return method;
    }

    /**
     * 获取当前class的全部方法
     */
    public List<MethodMetadata> getMethods() {
        return getMethods(null);
    }

    /**
     * 获取当前class的按指定规则过滤后的方法
     */
    public List<MethodMetadata> getMethods(Predicate<MethodMetadata> filter) {
        if (filter == null) {
            filter = f -> true;
        }
        List<MethodMetadata> methodMetadataList = new ArrayList<>();
        for (String fieldKey : methods.keySet()) {
            MethodMetadata methodMetadata = methods.get(fieldKey);
            if (filter.test(methodMetadata)) {
                methodMetadataList.add(methodMetadata);
            }
        }
        return methodMetadataList;
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> type) {
        return (A) classAnnotations.get(type);
    }

    public boolean hasAnnotation(Class<? extends Annotation> type) {
        return classAnnotations.containsKey(type);
    }

    /**
     * 收集类的字段
     */
    private void collectFields(Class<?> type) {
        if (type != null && type != Object.class && type != Record.class) {
            // 先收集父类字段
            collectFields(type.getSuperclass());

            Field[] fields = type.getDeclaredFields();

            // 由于getDeclaredFields返回的数组是无序，需要手动排序
            List<Field> fieldList = Arrays.stream(fields).sorted(fieldComparator).toList();
            for (Field field : fieldList) {
                try {
                    field.setAccessible(true);
                } catch (SecurityException e) {

                }

                //父类优先
                this.fields.put(field.getName(), new FieldMetadata(this, field));
            }
        }
    }

    private void collectConstructor(Class<?> type) {
        if (type != null && type != Object.class && type != Record.class) {
            // 优先收集父类的构造器
            collectConstructor(type.getSuperclass());
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                try {
                    constructor.setAccessible(true);
                } catch (SecurityException e) {

                }
                String key = buildMethodKey(type.getName(), constructor.getParameterTypes());
                //子类优先
                constructors.put(key, new ConstructorMetadata(constructor));
            }
        }
    }

    private void collectMethods(Class<?> type) {
        if (type != null && type != Object.class && type != Record.class) {
            // 优先收集父类的方法
            collectMethods(type.getSuperclass());
            Method[] methods = type.getDeclaredMethods();
            List<Method> methodList = Arrays.stream(methods)
                    .filter(i -> !ignoreMethodList.contains(i.getName()))
                    .sorted(methodComparator)
                    .toList();
            for (Method method : methodList) {
                try {
                    method.setAccessible(true);
                } catch (SecurityException e) {

                }
                String key = buildMethodKey(method.getName(), method.getParameterTypes());
                //子类优先
                this.methods.put(key, new MethodMetadata(this, method));
            }
        }
    }

    private String buildMethodKey(String methodName, Class<?>[] parameterTypes) {
        StringJoiner sj = new StringJoiner(",");
        //需要编译参数加 -parameters 参数才能获得参数名称
        for (Class<?> parameterType : parameterTypes) {
            //如果是基本类型，就转换为包装类型
//            Class<?> realType = Reflects.primitiveToWrapper(parameterType);
            Class<?> realType = parameterType.isPrimitive() ? PRIMITIVE_TO_WRAPPER.get(parameterType) : parameterType;
            sj.add(realType.getName());
        }
        return methodName + "(" + sj + ")";
    }

    private static int getOrderValue(Field field) {
        Index order = field.getAnnotation(Index.class);
        // 有注解返回注解值，没注解返回 Integer 最大值（排到最后）
        return (order != null) ? order.value() : Integer.MAX_VALUE;
    }
}
