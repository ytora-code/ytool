package xyz.ytora.ytool.json.config.convert.support;

import xyz.ytora.ytool.classcache.classmeta.ClassMetadata;
import xyz.ytora.ytool.classcache.classmeta.MethodMetadata;
import xyz.ytora.ytool.json.config.convert.JsonTypeConverter;
import xyz.ytora.ytool.json.context.JsonReadContext;
import xyz.ytora.ytool.json.context.JsonWriteContext;
import xyz.ytora.ytool.json.reader.JsonReader;

import java.lang.reflect.Type;

/**
 * created by yangtong on 2025/8/18 20:47:09
 * <br/>
 * 对于 MethodMetadata 类，希望直接返回方法名称
 */
public class MethodMetaConverter implements JsonTypeConverter<MethodMetadata> {

    @Override
    public MethodMetadata read(JsonReader r, Type declared, JsonReadContext ctx) {
        throw new UnsupportedOperationException("禁止反序列化 MethodMetadata 类型");
    }

    @Override
    public void write(StringBuilder out, MethodMetadata value, Type declared, JsonWriteContext ctx) {
        if (value == null) {
            out.append("null");
            return;
        }
        ctx.writeValue(out, value.getName(), String.class);
    }
}
