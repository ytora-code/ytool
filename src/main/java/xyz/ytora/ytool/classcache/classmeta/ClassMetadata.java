package xyz.ytora.ytool.classcache.classmeta;

import xyz.ytora.ytool.classcache.ClassCacheException;

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

            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                } catch (SecurityException e) {

                }

                //父类优先
                fields.put(field.getName(), new FieldMetadata(this, field));
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
            for (Method method : type.getDeclaredMethods()) {
                try {
                    method.setAccessible(true);
                } catch (SecurityException e) {

                }
                String key = buildMethodKey(method.getName(), method.getParameterTypes());
                //子类优先
                methods.put(key, new MethodMetadata(this, method));
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
}
