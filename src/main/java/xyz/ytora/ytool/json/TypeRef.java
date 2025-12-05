package xyz.ytora.ytool.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * created by yangtong on 2025/8/18 17:50:54
 * <br/>
 * 捕获运行时泛型类型的轻量工具,new TypeRef<List<String>>() {}
 */
public abstract class TypeRef<T> {
    private final Type type;

    protected TypeRef() {
        Type superClass = getClass().getGenericSuperclass();
        if (!(superClass instanceof ParameterizedType pt)) {
            throw new IllegalStateException(
                    "TypeRef must be created with anonymous subclass, e.g. new TypeRef<List<String>>() {}");
        }
        this.type = pt.getActualTypeArguments()[0];
    }

    /** 返回被捕获的完整 Type（可嵌套泛型） */
    public final Type type() {
        return type;
    }

    @Override
    public String toString() {
        return "TypeRef<" + type + ">";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof TypeRef<?> other) && Objects.equals(this.type, other.type);
    }
}
