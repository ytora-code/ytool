package xyz.ytora.ytool.json.config;

import xyz.ytora.ytool.classcache.classmeta.MethodMetadata;
import xyz.ytora.ytool.json.config.convert.ConverterRegistry;
import xyz.ytora.ytool.json.config.mapper.SetterFinder;

import java.util.Map;

/**
 * JSON解析器配置
 */
public final class JsonConfig {
    /**
     * 是否开启宽容模式
     */
    private final boolean lenient;
    /**
     * 类型转换器注册器
     */
    private final ConverterRegistry converters;
    /**
     * JSON字段 -> POJO字段的映射规则
     */
    private final SetterFinder setterFinder;

    private JsonConfig(boolean lenient, ConverterRegistry converters, SetterFinder setterFinder) {
        this.lenient = lenient;
        this.converters = converters;
        this.setterFinder = setterFinder;
    }

    public boolean lenient() {
        return lenient;
    }

    public ConverterRegistry converters() {
        return converters;
    }

    public MethodMetadata setterFinder(String name, Map<String, MethodMetadata> setters) {
        return setterFinder.getSetter(name, setters);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        // 默认宽松模式
        private boolean lenient = true;
        private ConverterRegistry converters = new ConverterRegistry();
        private SetterFinder setterFinder;

        public Builder lenient(boolean v) {
            this.lenient = v;
            return this;
        }

        public Builder converters(ConverterRegistry r) {
            this.converters = r;
            return this;
        }

        public Builder setterFinder(SetterFinder setterFinder) {
            this.setterFinder = setterFinder;
            return this;
        }

        public JsonConfig build() {
            return new JsonConfig(lenient, converters, setterFinder);
        }
    }
}
