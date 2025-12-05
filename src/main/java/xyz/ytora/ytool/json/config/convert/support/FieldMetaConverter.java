package xyz.ytora.ytool.json.config.convert.support;

import xyz.ytora.ytool.classcache.classmeta.ClassMetadata;
import xyz.ytora.ytool.classcache.classmeta.FieldMetadata;
import xyz.ytora.ytool.json.config.convert.JsonTypeConverter;
import xyz.ytora.ytool.json.context.JsonReadContext;
import xyz.ytora.ytool.json.context.JsonWriteContext;
import xyz.ytora.ytool.json.reader.JsonReader;

import java.lang.reflect.Type;

/**
 * created by yangtong on 2025/8/18 20:47:09
 * <br/>
 * 对于 FieldMetadata 类，希望直接返回字段名称
 */
public class FieldMetaConverter implements JsonTypeConverter<FieldMetadata> {

    @Override
    public FieldMetadata read(JsonReader r, Type declared, JsonReadContext ctx) {
        throw new UnsupportedOperationException("禁止反序列化 FieldMetadata 类型");
    }

    @Override
    public void write(StringBuilder out, FieldMetadata value, Type declared, JsonWriteContext ctx) {
        if (value == null) {
            out.append("null");
            return;
        }
        ctx.writeValue(out, value.getName(), String.class);
    }
}
