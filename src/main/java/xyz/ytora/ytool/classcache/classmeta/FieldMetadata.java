package xyz.ytora.ytool.classcache.classmeta;

import xyz.ytora.ytool.str.Strs;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字段元数据
 */
public class FieldMetadata {

    /**
     * 当前 FieldMetadata 所属的 ClassMetadata
     */
    private final ClassMetadata<?> classMetadata;

    /**
     * 字段名称
     */
    private final String name;
    /**
     * 原始字段反射对象
     */
    private final Field sourceField;
    /**
     * 注解集合
     */
    private final Map<Class<? extends Annotation>, Annotation> annotations;

    /**
     * 字段类型/修饰符，便于判断与日志
     */
    private final Class<?> type;

    /**
     * 修饰符
     */
    private final int modifiers;

    /**
     * VarHandle 缓存
     */
    private volatile VarHandle cachedHandle;

    /**
     * 该字段对于的 getter 方法
     */
    private MethodMetadata getter;

    /**
     * 该字段对于的 setter 方法
     */
    private MethodMetadata setter;

    public FieldMetadata(ClassMetadata<?> classMetadata, Field sourceField) {
        this.classMetadata = classMetadata;
        this.name = sourceField.getName();
        this.sourceField = sourceField;
        this.type = sourceField.getType();
        this.modifiers = sourceField.getModifiers();
        this.annotations = Arrays.stream(sourceField.getAnnotations())
                .collect(Collectors.toMap(Annotation::annotationType, a -> a));
    }

    /**
     * 获取 obj 对象的字段值
     */
    public <T> Object get(T obj) throws IllegalAccessException {
        try {
            VarHandle vh = ensureVarHandle();
            if (Modifier.isStatic(modifiers)) {
                return vh.get();
            } else {
                return vh.get(obj);
            }
        } catch (IllegalAccessException e) {
            throw e;
        } catch (Throwable t) {
            IllegalAccessException iae = new IllegalAccessException(t.getMessage());
            iae.initCause(t);
            throw iae;
        }
    }

    /**
     * 设置 obj 对象的字段值
     */
    public <T> void set(T obj, Object value) throws IllegalAccessException {
        try {
            VarHandle vh = ensureVarHandle();
            if (Modifier.isStatic(modifiers)) {
                vh.set(value);
            } else {
                vh.set(obj, value);
            }
        } catch (IllegalAccessException e) {
            throw e;
        } catch (Throwable t) {
            IllegalAccessException iae = new IllegalAccessException(t.getMessage());
            iae.initCause(t);
            throw iae;
        }
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> type) {
        return (A) annotations.get(type);
    }

    public boolean hasAnnotation(Class<? extends Annotation> type) {
        return annotations.containsKey(type);
    }

    public Type getGenericType() {
        return sourceField.getGenericType();
    }

    /* ============================ 字段修饰符 ============================ */

    public boolean isPublic() {
        return Modifier.isPublic(modifiers);
    }

    public boolean isProtected() {
        return Modifier.isProtected(modifiers);
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(modifiers);
    }

    public boolean isStatic() {
        return Modifier.isStatic(modifiers);
    }

    public boolean isFinal() {
        return Modifier.isFinal(modifiers);
    }

    public boolean isVolatile() {
        return Modifier.isVolatile(modifiers);
    }

    public boolean isTransient() {
        return Modifier.isTransient(modifiers);
    }

    /* ============================ setter ============================ */
    public MethodMetadata setter() {
        if (setter != null) {
            return setter;
        }
        setter = classMetadata.getMethod("set" + Strs.firstCapitalize(name));
        return setter;
    }

    /* ============================ getter ============================ */
    public MethodMetadata getter() {
        if (getter != null) {
            return getter;
        }
        getter = classMetadata.getMethod("get" + Strs.firstCapitalize(name));
        if (getter == null) {
            getter = classMetadata.getMethod("is" + Strs.firstCapitalize(name));
        }
        return getter;
    }

    public String getName() {
        return name;
    }

    public Field getSourceField() {
        return sourceField;
    }

    public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return annotations;
    }

    public Class<?> getType() {
        return type;
    }

    public int getModifiers() {
        return modifiers;
    }

    private VarHandle ensureVarHandle() throws IllegalAccessException {
        VarHandle vh = cachedHandle;
        if (vh != null) return vh;

        synchronized (this) {
            vh = cachedHandle;
            if (vh != null) return vh;

            try {
                // 使用 privateLookupIn 获取对声明类的“私有”访问权限的 Lookup
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                        sourceField.getDeclaringClass(), MethodHandles.lookup());
                vh = lookup.unreflectVarHandle(sourceField);
                cachedHandle = vh;
                return vh;
            } catch (IllegalAccessException | IllegalArgumentException e) {
                // 可选放宽：仅用于创建句柄；后续访问仍通过 VarHandle 完成
                try {
                    sourceField.setAccessible(true); // 按需启用（注意模块边界下仍可能需要 --add-opens）
                    MethodHandles.Lookup fallback = MethodHandles.lookup();
                    vh = fallback.unreflectVarHandle(sourceField);
                    cachedHandle = vh;
                    return vh;
                } catch (IllegalAccessException e2) {
                    IllegalAccessException iae = new IllegalAccessException(e2.getMessage());
                    iae.initCause(e2);
                    throw iae;
                }
            }
        }
    }
}
