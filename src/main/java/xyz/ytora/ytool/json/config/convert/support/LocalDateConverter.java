package xyz.ytora.ytool.json.config.convert.support;

import xyz.ytora.ytool.classcache.classmeta.ClassMetadata;
import xyz.ytora.ytool.json.JsonToken;
import xyz.ytora.ytool.json.config.convert.JsonTypeConverter;
import xyz.ytora.ytool.json.context.JsonReadContext;
import xyz.ytora.ytool.json.context.JsonWriteContext;
import xyz.ytora.ytool.json.reader.JsonReader;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * created by yangtong on 2025/8/18 20:47:09
 * <br/>
 */
public class LocalDateConverter implements JsonTypeConverter<LocalDate> {
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDate read(JsonReader r, Type declared, JsonReadContext ctx) {
        if (r.token() == JsonToken.VALUE_NULL) return null;
        String s = (r.token() == JsonToken.VALUE_STRING) ? r.string()
                : (String) ctx.readValue(String.class, r);
        return LocalDate.parse(s, F);
    }

    @Override
    public void write(StringBuilder out, LocalDate value, Type declared, JsonWriteContext ctx) {
        if (value == null) {
            out.append("null");
            return;
        }
        ctx.writeValue(out, value.format(F), String.class);
    }
}
