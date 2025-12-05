package xyz.ytora.ytool.json.config.convert.support;

import xyz.ytora.ytool.classcache.classmeta.ClassMetadata;
import xyz.ytora.ytool.json.JsonToken;
import xyz.ytora.ytool.json.config.convert.JsonTypeConverter;
import xyz.ytora.ytool.json.context.JsonReadContext;
import xyz.ytora.ytool.json.context.JsonWriteContext;
import xyz.ytora.ytool.json.reader.JsonReader;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * created by yangtong on 2025/8/18 20:47:09
 * <br/>
 */
public class LocalDateTimeConverter implements JsonTypeConverter<LocalDateTime> {
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime read(JsonReader r, Type declared, JsonReadContext ctx) {
        if (r.token() == JsonToken.VALUE_NULL) return null;
        String s = (r.token() == JsonToken.VALUE_STRING) ? r.string()
                : (String) ctx.readValue(String.class, r);
        if (s.indexOf('T') >= 0) s = s.replace('T', ' ');
        return LocalDateTime.parse(s, F);
    }

    @Override
    public void write(StringBuilder out, LocalDateTime value, Type declared, JsonWriteContext ctx) {
        if (value == null) {
            out.append("null");
            return;
        }
        ctx.writeValue(out, value.format(F), String.class);
    }
}
