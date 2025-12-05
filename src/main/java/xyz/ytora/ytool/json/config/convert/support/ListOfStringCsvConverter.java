package xyz.ytora.ytool.json.config.convert.support;

import xyz.ytora.ytool.classcache.classmeta.ClassMetadata;
import xyz.ytora.ytool.json.JsonParseException;
import xyz.ytora.ytool.json.JsonToken;
import xyz.ytora.ytool.json.config.convert.JsonTypeConverter;
import xyz.ytora.ytool.json.context.JsonReadContext;
import xyz.ytora.ytool.json.context.JsonWriteContext;
import xyz.ytora.ytool.json.reader.JsonReader;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * created by yangtong on 2025/8/18 20:47:48
 * <br/>
 */
public class ListOfStringCsvConverter implements JsonTypeConverter<List<String>> {

    private static boolean matches(Type t) {
        if (!(t instanceof ParameterizedType pt)) return false;
        if (!(pt.getRawType() instanceof Class<?> rc) || !List.class.isAssignableFrom(rc)) return false;
        Type elem = pt.getActualTypeArguments()[0];
        return (elem instanceof Class<?> ec) && ec == String.class;
    }

    @Override
    public List<String> read(JsonReader r, Type declared, JsonReadContext ctx) {
        if (!matches(declared)) {
            @SuppressWarnings("unchecked")
            List<String> v = (List<String>) ctx.readValue(declared, r);
            return v;
        }
        if (r.token() == JsonToken.VALUE_NULL) return null;

        if (r.token() == JsonToken.VALUE_STRING) {
            String s = r.string();
            if (s.isEmpty()) return new ArrayList<>();
            String[] parts = s.split(",");
            List<String> list = new ArrayList<>(parts.length);
            for (String p : parts) list.add(p);
            return list;
        } else if (r.token() == JsonToken.START_ARRAY) {
            // 不能通过ctx读取值，可能会产生无限递归
            //List<String> list = (List<String>) ctx.readValue(declared, r);
            List<String> list = new ArrayList<>();
            for (JsonToken t = r.next(); ; t = r.next()) {
                if (t == JsonToken.END_ARRAY || t == JsonToken.EOF) break;
                // 允许 null 元素：["a",null,"b"]
                if (t == JsonToken.VALUE_NULL) {
                    list.add(null);
                } else {
                    // 严格只接收字符串；若想宽松，改成 String.valueOf(ctx.readValue(Object.class, r))
                    if (t != JsonToken.VALUE_STRING) {
                        throw new JsonParseException("List<String> 的元素必须是字符串，实际: " + t);
                    }
                    list.add(r.string());
                }
            }
            return list;
        }
        throw new JsonParseException("List<String> 期望字符串或数组，实际: " + r.token());
    }

    @Override
    public void write(StringBuilder out, List<String> value, Type declared, JsonWriteContext ctx) {
        if (!matches(declared)) {
            ctx.writeValue(out, value, declared);
            return;
        }
        if (value == null) {
            out.append("null");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(value.get(i));
        }
        ctx.writeValue(out, sb.toString(), String.class);
    }
}