package xyz.ytora.ytool.classcache.classmeta;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 构造器元数据
 */
public class ConstructorMetadata<T> {
    /**
     * 构造器
     */
    private final Constructor<T> constructor;
    /**
     * 构造器参数
     */
    private final List<ParameterMetadata> parameters;
    /**
     * 构造器注解
     */
    private final Map<Class<? extends Annotation>, Annotation> annotations;

    /**
     * MethodHandle 缓存
     */
    private volatile MethodHandle cachedCtorHandle;
    private volatile MethodHandle cachedCtorHandleSpreader; // 将 Object[] 展开为形参的 spreader 句柄
    private final Object mhInitLock = new Object();

    public ConstructorMetadata(Constructor<T> constructor) {
        this.constructor = constructor;
        this.annotations = Arrays.stream(constructor.getAnnotations())
                .collect(Collectors.toMap(Annotation::annotationType, Function.identity()));

        this.parameters = new ArrayList<>();

        Parameter[] params = constructor.getParameters();
        Class<?>[] types = constructor.getParameterTypes();
        Annotation[][] paramAnn = constructor.getParameterAnnotations();
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            parameters.add(new ParameterMetadata(i, param.getName(), param.getType(), paramAnn[i]));
        }
    }

    /**
     * 使用 MethodHandle 实例化对象（非反射调用路径）。
     * - 0 参构造：走快速路径（不创建数组/不做展开）
     * - n 参构造：使用 asSpreader(Object[].class, n) 接受 Object[]，避免 invokeWithArguments 的额外装箱/组装开销
     * 与 Constructor.newInstance 的异常行为保持兼容：
     * - 访问问题抛 IllegalAccessException
     * - 目标构造抛出的异常被包裹为 InvocationTargetException
     * - 其他底层 Throwable 也包裹为 InvocationTargetException
     */
    @SuppressWarnings("unchecked")
    public T instance(Object... args)
            throws InvocationTargetException, InstantiationException, IllegalAccessException {
        try {
            int paramCount = constructor.getParameterCount();

            MethodHandle ctor = ensureCtorHandle();

            if (paramCount == 0) {
                // 快速路径：无参构造
                return (T) ctor.invoke(); // invokeExact 也可，但需精准 MethodType；invoke 由 JVM 做适配
            }

            // 懒构建一个 spreader 句柄，可以直接传 Object[]
            MethodHandle spreader = cachedCtorHandleSpreader;
            if (spreader == null) {
                synchronized (mhInitLock) {
                    spreader = cachedCtorHandleSpreader;
                    if (spreader == null) {
                        spreader = ctor.asSpreader(Object[].class, paramCount);
                        cachedCtorHandleSpreader = spreader;
                    }
                }
            }
            return (T) spreader.invoke(args);

        } catch (IllegalAccessException e) {
            // 无访问权限或模块边界问题
            throw e;
        } catch (InstantiationException e) {
            // 抽象类等不能实例化
            throw e;
        } catch (Throwable t) {
            // 目标构造器抛出的异常或其它运行期异常，按反射语义包裹
            throw new InvocationTargetException(t);
        }
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return annotationClass.cast(annotations.get(annotationClass));
    }

    public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
        return annotations.containsKey(annotationClass);
    }

    // ---------------- 构建/缓存构造器句柄 ----------------

    private MethodHandle ensureCtorHandle() throws IllegalAccessException, InstantiationException {
        MethodHandle mh = cachedCtorHandle;
        if (mh != null) return mh;

        synchronized (mhInitLock) {
            mh = cachedCtorHandle;
            if (mh != null) return mh;

            try {
                // 1. 先将构造器设置为可访问
                constructor.setAccessible(true);
                // 2. 将反射对象转换为 MethodHandle
                mh = MethodHandles.lookup().unreflectConstructor(constructor);

                cachedCtorHandle = mh;
                return mh;
            } catch (IllegalAccessException e) {
                throw e;
            } catch (SecurityException se) {
                // 安全管理器限制
                IllegalAccessException iae = new IllegalAccessException(se.getMessage());
                iae.initCause(se);
                throw iae;
            } catch (Throwable t) {
                // 其他异常包裹为 InstantiationException
                InstantiationException ie = new InstantiationException(t.getMessage());
                ie.initCause(t);
                throw ie;
            }
        }
    }
}
