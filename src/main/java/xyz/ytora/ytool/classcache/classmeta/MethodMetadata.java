package xyz.ytora.ytool.classcache.classmeta;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 方法元数据
 */
public class MethodMetadata {
    /**
     * 方法反射对象
     */
    private final Method method;
    /**
     * 方法上标注的注解
     */
    private final Map<Class<? extends Annotation>, Annotation> annotations;
    /**
     * 方法名称
     */
    private final String methodName;
    /**
     * 方法参数
     */
    private final List<ParameterMetadata> parameters;

    /**
     * 带泛型的参数类型
     */
    private final Type[] genericParameterTypes;

    /**
     * 方法返回值类型
     */
    private final Class<?> returnType;

    /**
     * 带泛型方法返回值类型
     */
    private final Type genericReturnType;

    /**
     * 方法返回值是否是聚合类型（数组、集合类型）
     */
    private volatile Boolean cachedIsAggregateType;

    /**
     * 聚合类型元素类型
     */
    private volatile Class<?> cachedElementType;

    /**
     * 修饰符
     */
    private final Integer modifiers;

    /**
     * MethodHandle缓存
     */
    private volatile MethodHandle cachedHandle;

    public MethodMetadata(Method method) {
        this.method = method;
        this.methodName = method.getName();
        this.annotations = Arrays.stream(method.getAnnotations())
                .collect(Collectors.toMap(Annotation::annotationType, a -> a));

        this.parameters = new ArrayList<>();
        this.genericParameterTypes = method.getGenericParameterTypes();
        this.returnType = method.getReturnType();
        this.genericReturnType = method.getGenericReturnType();
        this.modifiers = method.getModifiers();

        Parameter[] params = method.getParameters();
        Annotation[][] paramAnn = method.getParameterAnnotations();
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            parameters.add(new ParameterMetadata(i, param.getName(), param.getType(), paramAnn[i]));
        }
    }

    /**
     * 通过 MethodHandle 调用方法
     */
    public <T> Object invoke(T obj, Object... args) throws InvocationTargetException, IllegalAccessException {
        try {
            MethodHandle mh = cachedHandle;
            if (mh == null) {
                synchronized (this) {
                    mh = cachedHandle;
                    if (mh == null) {
                        // 如需支持访问非 public 成员，可能需要在模块/包层面开放访问；
                        // 可在此处 method.setAccessible(true) 再 unreflect。
                        // method.setAccessible(true); // 可选：按需启用
                        mh = MethodHandles.lookup().unreflect(method);
                        cachedHandle = mh;
                    }
                }
            }

            // 组装调用参数
            Object result;
            if (Modifier.isStatic(modifiers)) {
                // 静态方法：直接传入形参
                result = mh.invokeWithArguments(args);
            } else {
                // 实例方法：将 obj 作为第一个参数
                Object[] full = new Object[(args == null ? 0 : args.length) + 1];
                full[0] = obj;
                if (args != null && args.length > 0) {
                    System.arraycopy(args, 0, full, 1, args.length);
                }
                result = mh.invokeWithArguments(full);
            }
            return result;
        } catch (IllegalAccessException e) {
            // 句柄创建阶段或访问受限
            throw e;
        } catch (Throwable t) {
            // 与 Method.invoke 行为保持一致：将目标方法抛出的异常包裹
            throw new InvocationTargetException(t);
        }
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> type) {
        return (A) annotations.get(type);
    }

    public Boolean hasAnnotation(Class<? extends Annotation> type) {
        return annotations.containsKey(type);
    }

    /**
     * 获取原始method对象
     */
    public Method getOriginMethod() {
        return this.method;
    }

    /**
     * 获取方法名称
     */
    public String getName() {
        return this.methodName;
    }

    /**
     * 获取方法参数列表
     */
    public List<ParameterMetadata> parameters() {
        return this.parameters;
    }

    /*================================== 访问权限 =============================================*/

    /**
     * 判断方法是否为 public 修饰。
     */
    public Boolean isPublic() {
        return Modifier.isPublic(modifiers);
    }

    /**
     * 判断方法是否为 protected 修饰。
     */
    public boolean isProtected() {
        return Modifier.isProtected(modifiers);
    }

    /**
     * 判断方法是否为 private 修饰。
     */
    public boolean isPrivate() {
        return Modifier.isPrivate(modifiers);
    }

    /**
     * 判断方法是否为包级私有（缺省访问修饰符）。
     */
    public boolean isDefault() {
        return !isPublic() && !isProtected() && !isPrivate();
    }


    /*================================== 修饰符 =============================================*/

    /**
     * 判断方法是否为 static 修饰。
     */
    public boolean isStatic() {
        return Modifier.isStatic(modifiers);
    }

    /**
     * 判断方法是否为 final 修饰。
     */
    public boolean isFinal() {
        return Modifier.isFinal(modifiers);
    }

    /**
     * 判断方法是否为 abstract 修饰。
     */
    public boolean isAbstract() {
        return Modifier.isAbstract(modifiers);
    }

    /**
     * 判断方法是否为 synchronized 修饰。
     */
    public boolean isSynchronized() {
        return Modifier.isSynchronized(modifiers);
    }

    /**
     * 判断方法是否为 native 修饰。
     */
    public boolean isNative() {
        return Modifier.isNative(modifiers);
    }

    /**
     * 判断方法是否为 strictfp 修饰。
     */
    public boolean isStrictfp() {
        return Modifier.isStrict(modifiers);
    }

    /**
     * 判断方法是否为接口的默认方法。
     */
    public boolean isDefaultMethod() {
        return method.isDefault();
    }

    /**
     * 判断方法是否为编译器生成的桥接方法。
     * <p>桥接方法（Bridge Method）是 Java 编译器为支持泛型类型擦除和多态而生成的隐藏方法，
     * 例如当子类重写泛型方法时可能生成。</p>
     * 编译器自动生成
     */
    public boolean isBridge() {
        return method.isBridge();
    }

    /**
     * 判断方法是否为可变参数方法。
     * <p>可变参数方法允许调用时传入任意数量的实参，
     * 在方法内部会被视为数组处理。</p>
     */
    public boolean isVarArgs() {
        return method.isVarArgs();
    }

    /**
     * 判断方法是否为合成方法（Synthetic）。
     * <p>合成方法是编译器自动生成的、在源码中不可见的方法，
     * 常用于访问内部类的私有成员等情况。</p>
     * 编译器自动生成
     */
    public boolean isSynthetic() {
        return method.isSynthetic();
    }

    public Class<?> returnType() {
        return returnType;
    }

    public Type[] genericParameterTypes() {
        return genericParameterTypes;
    }

    public Type genericReturnType() {
        return genericReturnType;
    }

    /**
     * 判断方法返回值是否为聚合类型（数组、List、Set等Collection）
     */
    public boolean isAggregateReturnType() {
        if (cachedIsAggregateType == null) {
            // 检查是否为数组类型
            if (returnType.isArray()) {
                cachedIsAggregateType = true;
            } else {
                // 检查是否为集合类型
                cachedIsAggregateType = isCollectionType(returnType);
            }
        }
        return cachedIsAggregateType;
    }

    /**
     * 提取聚合类型的元素类型
     * 如果方法返回值是List、Set、数组等聚合类型，则返回其元素类型
     * 例如：List<User> 返回 User.class，String[] 返回 String.class
     *
     * @return 聚合类型的元素类型，如果不是聚合类型则返回原返回值类型
     */
    public Class<?> getElementType() {
        if (cachedElementType == null) {
            cachedElementType = extractElementTypeInternal();
        }
        return cachedElementType;
    }

    /**
     * 内部方法：提取元素类型的具体实现
     */
    private Class<?> extractElementTypeInternal() {
        Type returnGenericType = method.getGenericReturnType();

        // 处理数组类型
        if (returnType.isArray()) {
            return returnType.getComponentType();
        }

        // 处理泛型集合类型（List、Set等）
        if (isCollectionType(returnType) && returnGenericType instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (actualTypeArguments.length > 0) {
                Type elementType = actualTypeArguments[0];

                // 如果泛型参数是Class类型，直接返回
                if (elementType instanceof Class) {
                    return (Class<?>) elementType;
                }

                // 如果泛型参数是ParameterizedType，返回其原始类型
                if (elementType instanceof ParameterizedType paramElementType) {
                    return (Class<?>) paramElementType.getRawType();
                }
            }
        }

        // 如果不是聚合类型或无法确定元素类型，返回原始类型
        return returnType;
    }

    /**
     * 判断给定的类型是否为集合类型
     */
    private static boolean isCollectionType(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }
}
