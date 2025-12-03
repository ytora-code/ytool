package xyz.ytora.ytool.classcache.classmeta;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 方法参数元数据
 */
public class ParameterMetadata {
    /**
     * 参数下标，从0开始
     */
    private final int index;
    /**
     * 参数名称
     */
    private final String name;
    /**
     * 参数类型
     */
    private final Class<?> type;
    /**
     * 参数注解
     */
    private final Map<Class<? extends Annotation>, Annotation> annotations;

    public ParameterMetadata(int index, String name, Class<?> type, Annotation[] rawAnnotations) {
        this.index = index;
        this.name = name;
        this.type = type;
        this.annotations = Arrays.stream(rawAnnotations)
                .collect(Collectors.toMap(Annotation::annotationType, Function.identity()));
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> type) {
        return (A) annotations.get(type);
    }

    public boolean hasAnnotation(Class<? extends Annotation> type) {
        return annotations.containsKey(type);
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return annotations;
    }
}
