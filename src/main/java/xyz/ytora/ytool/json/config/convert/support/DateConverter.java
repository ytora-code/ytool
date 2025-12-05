package xyz.ytora.ytool.json.config.convert.support;

import xyz.ytora.ytool.classcache.classmeta.ClassMetadata;
import xyz.ytora.ytool.date.Dates;
import xyz.ytora.ytool.json.JsonToken;
import xyz.ytora.ytool.json.config.convert.JsonTypeConverter;
import xyz.ytora.ytool.json.context.JsonReadContext;
import xyz.ytora.ytool.json.context.JsonWriteContext;
import xyz.ytora.ytool.json.reader.JsonReader;

import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * created by yangtong on 2025/8/18 20:47:09
 * <br/>
 */
public class DateConverter implements JsonTypeConverter<Date> {
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Date read(JsonReader r, Type declared, JsonReadContext ctx) {
        if (r.token() == JsonToken.VALUE_NULL) return null;
        String s = (r.token() == JsonToken.VALUE_STRING) ? r.string()
                : (String) ctx.readValue(String.class, r);
        return Dates.parseDate(s);
    }

    @Override
    public void write(StringBuilder out, Date value, Type declared, JsonWriteContext ctx) {
        if (value == null) {
            out.append("null");
            return;
        }
        ctx.writeValue(out, Dates.formatDate(value), String.class);
    }
}
