package xyz.ytora.ytool.json;

import xyz.ytora.ytool.classcache.ClassCache;
import xyz.ytora.ytool.classcache.classmeta.ClassMetadata;
import xyz.ytora.ytool.classcache.classmeta.ConstructorMetadata;
import xyz.ytora.ytool.classcache.classmeta.FieldMetadata;
import xyz.ytora.ytool.classcache.classmeta.MethodMetadata;
import xyz.ytora.ytool.json.config.JsonConfig;
import xyz.ytora.ytool.json.config.convert.ConverterRegistry;
import xyz.ytora.ytool.json.config.convert.JsonTypeConverter;
import xyz.ytora.ytool.json.config.convert.support.*;
import xyz.ytora.ytool.json.config.mapper.DefaultSetterFinder;
import xyz.ytora.ytool.json.context.JsonReadContext;
import xyz.ytora.ytool.json.context.JsonWriteContext;
import xyz.ytora.ytool.json.reader.JsonReader;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JSON字符串 ↔ POJO
 * JSON字节数组 ↔ POJO
 * JSON流 ↔ POJO
 */
public class JsonMapper {

    private final JsonConfig config;

    public static JsonMapper createDefault() {
        // 类型转换器
        ConverterRegistry registry = new ConverterRegistry();
        registry.register(FieldMetadata.class, new FieldMetaConverter());
        registry.register(MethodMetadata.class, new MethodMetaConverter());
        registry.register(ClassMetadata.class, new ClassMetaConverter());
        registry.register(LocalDate.class, new LocalDateConverter());
        registry.register(LocalDateTime.class, new LocalDateTimeConverter());
        registry.register(Date.class, new DateConverter());
        // 带泛型的List类型：List<String>
        registry.register(new TypeRef<List<String>>() { }, new ListOfStringCsvConverter());

        JsonConfig config = JsonConfig.builder()
                .lenient(true)
                .converters(registry)
                .setterFinder(new DefaultSetterFinder())
                .build();
        return new JsonMapper(config);
    }

    public JsonMapper(JsonConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /* ====================== 公共 API ====================== */

    /**
     * 序列化 pojo -> json
     */
    public String toJson(Object bean) {
        StringBuilder sb = new StringBuilder(256);
        writeCtx().writeValue(sb, bean, null);
        return sb.toString();
    }

    /**
     * 反序列化 json -> pojo
     */
    public <T> T fromJson(String json, Class<T> type) {
        JsonReader r = new JsonReader(json, config.lenient());
        r.next();
        @SuppressWarnings("unchecked")
        T v = (T) readValue(type, r);
        return v;
    }

    /**
     * 反序列化 json -> pojo
     */
    public Object fromJson(String json, Type type) {
        JsonReader r = new JsonReader(json, config.lenient());
        r.next();
        return readValue(type, r);
    }

    /**
     * 带泛型的反序列化 json -> pojo
     */
    public <T> T fromJson(String json, TypeRef<T> ref) {
        JsonReader r = new JsonReader(json, config.lenient());
        r.next();
        @SuppressWarnings("unchecked")
        T v = (T) readValue(ref.type(), r);
        return v;
    }

    /* ====================== 上下文实现 ====================== */

    private JsonReadContext readCtx() {
        return this::readValue;
    }

    private JsonWriteContext writeCtx() {
        ConverterRegistry registry = config.converters();
        return new JsonWriteContext() {
            @Override
            public void writeValue(StringBuilder out, Object value, Type declaredType) {
                // 1) 声明类型优先（泛型）
                if (declaredType != null) {
                    JsonTypeConverter<Object> c = cast(registry.lookup(declaredType));
                    if (c != null) {
                        c.write(out, value, declaredType, this);
                        return;
                    }

                    if (declaredType instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> rc) {
                        JsonTypeConverter<Object> c2 = cast(registry.lookup(rc));
                        if (c2 != null) {
                            c2.write(out, value, declaredType, this);
                            return;
                        }
                    } else if (declaredType instanceof Class<?> rc2) {
                        JsonTypeConverter<Object> c3 = cast(registry.lookup(rc2));
                        if (c3 != null) {
                            c3.write(out, value, declaredType, this);
                            return;
                        }
                    }
                }

                // 2) 运行时类型
                if (value != null) {
                    JsonTypeConverter<Object> c4 = cast(registry.lookup(value.getClass()));
                    if (c4 != null) {
                        c4.write(out, value, value.getClass(), this);
                        return;
                    }
                }

                // 3) 默认写
                JsonMapper.this.writeValue(out, value, this);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> JsonTypeConverter<T> cast(JsonTypeConverter<?> c) {
        return (JsonTypeConverter<T>) c;
    }

    /* ====================== 核心写入 ====================== */

    private void writeValue(StringBuilder out, Object v, JsonWriteContext ctx) {
        if (v == null) {
            out.append("null");
            return;
        }

        // 标量
        if (v instanceof String s) {
            writeJsonString(out, s);
            return;
        }
        if (v instanceof Boolean b) {
            out.append(b ? "true" : "false");
            return;
        }
        if (v instanceof Number n) {
            writeNumber(out, n);
            return;
        }
        if (v.getClass().isEnum()) {
            writeJsonString(out, ((Enum<?>) v).name());
            return;
        }

        Class<?> c = v.getClass();

        // 数组
        if (c.isArray()) {
            out.append('[');
            int len = Array.getLength(v);
            for (int i = 0; i < len; i++) {
                if (i > 0) out.append(',');
                ctx.writeValue(out, Array.get(v, i), c.getComponentType());
            }
            out.append(']');
            return;
        }

        // Collection
        if (v instanceof Collection<?> coll) {
            out.append('[');
            boolean first = true;
            for (Object e : coll) {
                if (!first) out.append(',');
                first = false;
                ctx.writeValue(out, e, null);
            }
            out.append(']');
            return;
        }

        // Map
        if (v instanceof Map<?, ?> m) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> en : m.entrySet()) {
                if (!first) out.append(',');
                first = false;
                writeJsonString(out, String.valueOf(en.getKey()));
                out.append(':');
                ctx.writeValue(out, en.getValue(), null);
            }
            out.append('}');
            return;
        }

        if (isPlatformClass(v.getClass())) {
            writeJsonString(out, String.valueOf(v));
            return;
        }

        // POJO
        writePojo(out, v, ctx);
    }

    private void writePojo(StringBuilder out, Object bean, JsonWriteContext ctx) {
        Class<?> c = bean.getClass();
        Map<String, GetterInfo> getters = findGetters(c);
        ConverterRegistry registry = config.converters();

        out.append('{');
        boolean first = true;
        for (Map.Entry<String, GetterInfo> en : getters.entrySet()) {
            String prop = en.getKey();
            GetterInfo gi = en.getValue();

            Object val;
            try {
                val = gi.method.invoke(bean);
            } catch (Exception e) {
                throw error("getter 执行失败: " + gi.name + " -> " + e.getMessage());
            }

            if (!first) out.append(',');
            first = false;
            writeJsonString(out, prop);
            out.append(':');

            // 优先按 getter 的“声明泛型类型”找转换器
            JsonTypeConverter<Object> cvr = cast(registry.lookup(gi.genericReturnType));
            if (cvr != null) {
                cvr.write(out, val, gi.genericReturnType, ctx);
            } else {
                ctx.writeValue(out, val, null);
            }
        }
        out.append('}');
    }

    private static final class GetterInfo {
        final String name;
        final MethodMetadata method;
        final Type genericReturnType;

        GetterInfo(String name, MethodMetadata mm, Type grt) {
            this.name = name;
            this.method = mm;
            this.genericReturnType = grt;
        }
    }

    private Map<String, GetterInfo> findGetters(Class<?> c) {
        if (isPlatformClass(c)) {
            return Collections.emptyMap();
        }
        Map<String, GetterInfo> map = new LinkedHashMap<>();
        ClassMetadata<?> classMetadata = ClassCache.get(c);
        for (MethodMetadata mm : classMetadata.getMethods()) {
            if (mm.isStatic() || !mm.isPublic() || !mm.parameters().isEmpty()) continue;
            String name = mm.getName();

            String prop = null;
            // record类没有getter，直接获取record组件对应的方法
            if (c.isRecord()) {
                if (!classMetadata.getSourceFieldMap().containsKey(name)) {
                    continue;
                }
                prop = name;
            }
            // 普通类，解构getter方法
            else {
                if (name.startsWith("get") && name.length() > 3) prop = decap(name.substring(3));
                else if (name.startsWith("is") && name.length() > 2
                        && (mm.returnType() == boolean.class || mm.returnType() == Boolean.class)) {
                    prop = decap(name.substring(2));
                }
            }
            if (prop == null || "class".equals(prop)) continue;

            map.put(prop, new GetterInfo(name, mm, mm.genericReturnType()));
        }
        return map;
    }

    private void writeNumber(StringBuilder out, Number n) {
        if (n instanceof Double d) {
            if (d.isNaN() || d.isInfinite()) {
                out.append("null");
                return;
            }
        } else if (n instanceof Float f) {
            if (f.isNaN() || f.isInfinite()) {
                out.append("null");
                return;
            }
        }
        out.append(n.toString());
    }

    private void writeJsonString(StringBuilder sb, String s) {
        if (s == null) {
            sb.append("null");
            return;
        }
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    /* ====================== 核心读取 ====================== */

    private Object readValue(Type targetType, JsonReader r) {
        ConverterRegistry registry = config.converters();

        // 0) 判断该类型有没有注册类型转换器
        JsonTypeConverter<Object> cv = cast(registry.lookup(targetType));
        if (cv != null) {
            return cv.read(r, targetType, readCtx());
        }

        // 1) 判断该字段类型是否属于普通类
        if (targetType instanceof Class<?> cls) {
            JsonTypeConverter<Object> rawCv = cast(registry.lookup(cls));
            if (rawCv != null) return rawCv.read(r, cls, readCtx());
            if (cls == Object.class) return readAny(r);

            // 如果该字段是标量
            if (cls == String.class) return readString(r);
            if (cls == boolean.class || cls == Boolean.class) return readBoolean(r);
            if (isIntLike(cls)) return readInt(r);
            if (isLongLike(cls)) return readLong(r);
            if (isDoubleLike(cls)) return readDouble(r);
            if (isFloatLike(cls)) return readFloat(r);
            if (isShortLike(cls)) return readShort(r);
            if (isByteLike(cls)) return readByte(r);
            if (cls == BigInteger.class) return readBigInteger(r);
            if (cls == BigDecimal.class) return readBigDecimal(r);
            if (cls.isEnum()) return readEnum(cls, r);

            // 如果是数组
            if (cls.isArray()) {
                return readArray(cls.getComponentType(), r);
            }
            // 集合/字典（无泛型）
            if (Collection.class.isAssignableFrom(cls)) {
                return readCollection((Class<? extends Collection>) cls, Object.class, r);
            }
            if (Map.class.isAssignableFrom(cls)) {
                return readMap((Class<? extends Map>) cls, Object.class, r);
            }

            // 如果该字段类型是JDK平台类型，直接返回NULL。如果平台类型注册了类型转换器，不会走到这
            if (isPlatformClass(cls)) {
                return null;
            }

            // 如果该字段是POJO
            return bindPojo(cls, r);
        }

        // 判断该字段类型是否属于泛型类
        if (targetType instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            if (raw instanceof Class<?> rawClass) {
                if (Collection.class.isAssignableFrom(rawClass)) {
                    Type elem = pt.getActualTypeArguments()[0];
                    Class<? extends Collection> concrete;
                    if (rawClass == List.class || rawClass == Collection.class) concrete = ArrayList.class;
                    else if (rawClass == Set.class) concrete = LinkedHashSet.class;
                    else {
                        @SuppressWarnings("unchecked") Class<? extends Collection> c = (Class<? extends Collection>) rawClass;
                        concrete = c;
                    }
                    return readCollection(concrete, elem, r);
                }
                if (Map.class.isAssignableFrom(rawClass)) {
                    Type keyType = pt.getActualTypeArguments()[0];
                    Type vType = pt.getActualTypeArguments()[1];
                    if (!(keyType instanceof Class<?> k) || k != String.class)
                        throw error("仅支持 Map<String, V> 的键类型");
                    Class<? extends Map> concrete;
                    if (rawClass == Map.class) concrete = LinkedHashMap.class;
                    else {
                        @SuppressWarnings("unchecked") Class<? extends Map> c = (Class<? extends Map>) rawClass;
                        concrete = c;
                    }
                    return readMap(concrete, vType, r);
                }
                // 泛型 POJO：setter 参数上仍可取到泛型
                return bindPojo(rawClass, r);
            }
        }

        return readAny(r);
    }

    // —— 标量 —— //
    private String readString(JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return null;
        if (r.token() != JsonToken.VALUE_STRING) throw error("期望字符串，实际: " + r.token());
        return r.string();
    }

    private boolean readBoolean(JsonReader r) {
        if (r.token() == JsonToken.VALUE_BOOLEAN) return r.boolVal();
        if (r.token() == JsonToken.VALUE_NULL) return false;
        throw error("期望布尔，实际: " + r.token());
    }

    private int readInt(JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return 0;
        if (r.token() != JsonToken.VALUE_NUMBER) throw error("期望数字(int)");
        return r.isDoubleNumber() ? (int) r.doubleVal() : (int) r.longVal();
    }

    private long readLong(JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return 0L;
        if (r.token() != JsonToken.VALUE_NUMBER) throw error("期望数字(long)");
        return r.isDoubleNumber() ? (long) r.doubleVal() : r.longVal();
    }

    private double readDouble(JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return 0d;
        if (r.token() != JsonToken.VALUE_NUMBER) throw error("期望数字(double)");
        return r.isDoubleNumber() ? r.doubleVal() : (double) r.longVal();
    }

    private float readFloat(JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return 0f;
        if (r.token() != JsonToken.VALUE_NUMBER) throw error("期望数字(float)");
        return r.isDoubleNumber() ? (float) r.doubleVal() : (float) r.longVal();
    }

    private short readShort(JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return 0;
        if (r.token() != JsonToken.VALUE_NUMBER) throw error("期望数字(short)");
        return r.isDoubleNumber() ? (short) r.doubleVal() : (short) r.longVal();
    }

    private byte readByte(JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return 0;
        if (r.token() != JsonToken.VALUE_NUMBER) throw error("期望数字(byte)");
        return r.isDoubleNumber() ? (byte) r.doubleVal() : (byte) r.longVal();
    }

    private BigInteger readBigInteger(JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return null;
        if (r.token() != JsonToken.VALUE_NUMBER) throw error("期望数字(BigInteger)");
        if (r.isDoubleNumber()) return BigDecimal.valueOf(r.doubleVal()).toBigInteger();
        return BigInteger.valueOf(r.longVal());
    }

    private BigDecimal readBigDecimal(JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return null;
        if (r.token() != JsonToken.VALUE_NUMBER) throw error("期望数字(BigDecimal)");
        return r.isDoubleNumber() ? BigDecimal.valueOf(r.doubleVal()) : BigDecimal.valueOf(r.longVal());
    }

    private Object readEnum(Class<?> enumClass, JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return null;
        if (r.token() != JsonToken.VALUE_STRING) throw error("枚举需字符串");
        String name = r.string();
        @SuppressWarnings("unchecked")
        Object v = Enum.valueOf((Class<? extends Enum>) enumClass, name);
        return v;
    }

    // —— 结构 —— //
    private Object readArray(Class<?> compType, JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return null;
        if (r.token() != JsonToken.START_ARRAY) throw error("期望数组开始");
        List<Object> tmp = new ArrayList<>();
        for (JsonToken t = r.next(); ; t = r.next()) {
            if (t == JsonToken.END_ARRAY || t == JsonToken.EOF) break;
            tmp.add(readCtx().readValue(compType, r));
        }
        Object arr = Array.newInstance(compType, tmp.size());
        for (int i = 0; i < tmp.size(); i++) Array.set(arr, i, tmp.get(i));
        return arr;
    }

    private Collection<?> readCollection(Class<? extends Collection> raw, Type elemType, JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return null;
        if (r.token() != JsonToken.START_ARRAY) throw error("期望数组开始");
        Collection<Object> coll = newCollection(raw);
        for (JsonToken t = r.next(); ; t = r.next()) {
            if (t == JsonToken.END_ARRAY || t == JsonToken.EOF) break;
            coll.add(readCtx().readValue(elemType, r));
        }
        return coll;
    }

    private Map<?, ?> readMap(Class<? extends Map> raw, Type vType, JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return null;
        if (r.token() != JsonToken.START_OBJECT) throw error("期望对象开始");
        Map<String, Object> map = newMap(raw);
        for (JsonToken t = r.next(); ; t = r.next()) {
            if (t == JsonToken.END_OBJECT || t == JsonToken.EOF) break;
            if (t != JsonToken.FIELD_NAME && t != JsonToken.VALUE_STRING) throw error("期望字段名");
            String key = r.string();
            t = r.next();
            if (t == JsonToken.EOF) break;
            map.put(key, readCtx().readValue(vType, r));
        }
        return map;
    }

    private <T> T bindPojo(Class<T> cls, JsonReader r) {
        if (r.token() == JsonToken.VALUE_NULL) return null;
        if (r.token() != JsonToken.START_OBJECT) throw error("期望对象开始, 实际却读到:" + r.token());

        // 获取类元缓存
        ClassMetadata<T> classMetadata = ClassCache.get(cls);
        final T bean;
        try {
            ConstructorMetadata<T> constructor = classMetadata.getConstructor();
            bean = constructor.instance();
        } catch (Exception e) {
            throw error("实例化失败: " + e.getMessage());
        }

        Map<String, MethodMetadata> setters = findSetters(cls, classMetadata);
        ConverterRegistry registry = config.converters();

        for (JsonToken t = r.next(); ; t = r.next()) {
            if (t == JsonToken.END_OBJECT || t == JsonToken.EOF) break;
            if (t != JsonToken.FIELD_NAME && t != JsonToken.VALUE_STRING) throw error("期望字段名");
            String key = r.string();

            t = r.next();
            if (t == JsonToken.EOF) break;

            MethodMetadata setter = config.setterFinder(key, setters);
            if (setter == null) {
                skipValue(r);
                continue;
            }

            Type paramType = setter.genericParameterTypes()[0];

            // 将JSON字段的值绑定到POJO（通过setter）
            JsonTypeConverter<Object> cv = cast(registry.lookup(paramType));
            Object arg = (cv != null) ? cv.read(r, paramType, readCtx()) : readCtx().readValue(paramType, r);
            try {
                setter.invoke(bean, arg);
            } catch (Exception e) {
                throw error("调用 setter 失败: " + setter.getName() + " -> " + e.getMessage());
            }
        }
        return bean;
    }

    private <T> Map<String, MethodMetadata> findSetters(Class<?> c, ClassMetadata<T> classMetadata) {
        if (isPlatformClass(c)) {
            return Collections.emptyMap();
        }
        Map<String, MethodMetadata> map = new HashMap<>();
        for (MethodMetadata method : classMetadata.getMethods()) {
            if (method.isStatic() || !method.isPublic() || method.parameters().size() != 1) continue;
            String name = method.getName();
            if (name.startsWith("set") && name.length() > 3) {
                String prop = decap(name.substring(3));
                map.put(prop, method);
            }
        }
        return map;
    }

    private static String decap(String s) {
        if (s == null || s.isEmpty()) return s;
        char c0 = s.charAt(0);
        char lc = Character.toLowerCase(c0);
        return (c0 == lc) ? s : lc + s.substring(1);
    }

    void skipValue(JsonReader r) {
        JsonToken t = r.token();
        // 若还未取到任何 token，先推进一次，保证进入时有“当前值”
        if (t == null) {
            t = r.next();
            if (t == null) {
                // 输入已结束，直接返回
                return;
            }
        }

        switch (t) {
            case START_OBJECT: {
                int depth = 1;
                // 仅在循环内推进，让深度回到 0 时停在 END_OBJECT 上返回
                while (depth > 0) {
                    JsonToken nt = r.next();
                    // 健壮性：若数据提前结束，直接返回（也可选择抛错）
                    if (nt == null) {
                        return;
                    }
                    if (nt == JsonToken.START_OBJECT || nt == JsonToken.START_ARRAY) {
                        depth++;
                    } else if (nt == JsonToken.END_OBJECT || nt == JsonToken.END_ARRAY) {
                        depth--;
                    }
                }
                // 此时 r.token() == END_OBJECT；不要再 next()！
                return;
            }
            case START_ARRAY: {
                int depth = 1;
                while (depth > 0) {
                    JsonToken nt = r.next();
                    if (nt == null) {
                        return;
                    }
                    if (nt == JsonToken.START_OBJECT || nt == JsonToken.START_ARRAY) {
                        depth++;
                    } else if (nt == JsonToken.END_OBJECT || nt == JsonToken.END_ARRAY) {
                        depth--;
                    }
                }
                // 此时 r.token() == END_ARRAY；不要再 next()！
                return;
            }
            case FIELD_NAME: {
                // 兼容：若误在字段名上调用，推进到“值”，然后仅跳过这一个值
                JsonToken v = r.next(); // 到达该字段的值（可能是标量/对象/数组）
                if (v == null) return;
                // 递归一次：递归版本会在对象/数组时停在其 END_* 上，在标量时停在当前值上
                skipValue(r);
                return;
            }
            default:
                // 标量或 NULL：保持当前位置（不前进），留给上层的 next() 去吃逗号/空白
        }
    }


    private Collection<Object> newCollection(Class<? extends Collection> raw) {
        if (raw == List.class || raw == Collection.class || raw == ArrayList.class) return new ArrayList<>();
        if (raw == Set.class || raw == HashSet.class || raw == LinkedHashSet.class) return new LinkedHashSet<>();
        if (raw == Queue.class || raw == Deque.class || raw == LinkedList.class) return new LinkedList<>();
        try {
            @SuppressWarnings("unchecked")
            Collection<Object> c = (Collection<Object>) raw.getDeclaredConstructor().newInstance();
            return c;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, Object> newMap(Class<? extends Map> raw) {
        if (raw == Map.class || raw == LinkedHashMap.class) return new LinkedHashMap<>();
        if (raw == HashMap.class) return new HashMap<>();
        if (raw == TreeMap.class) return new TreeMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) raw.getDeclaredConstructor().newInstance();
            return m;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private static boolean isIntLike(Class<?> c) {
        return c == int.class || c == Integer.class;
    }

    private static boolean isLongLike(Class<?> c) {
        return c == long.class || c == Long.class;
    }

    private static boolean isDoubleLike(Class<?> c) {
        return c == double.class || c == Double.class;
    }

    private static boolean isFloatLike(Class<?> c) {
        return c == float.class || c == Float.class;
    }

    private static boolean isShortLike(Class<?> c) {
        return c == short.class || c == Short.class;
    }

    private static boolean isByteLike(Class<?> c) {
        return c == byte.class || c == Byte.class;
    }

    private static RuntimeException error(String msg) {
        return new JsonParseException(msg);
    }

    // 顶层动态
    private Object readAny(JsonReader r) {
        switch (r.token()) {
            case VALUE_NULL:
                return null;
            case VALUE_STRING:
                return r.string();
            case VALUE_BOOLEAN:
                return r.boolVal();
            case VALUE_NUMBER: {
                if (r.isDoubleNumber()) {
                    return r.doubleVal();
                } else {
                    long L = r.longVal();
                    if (L >= Integer.MIN_VALUE && L <= Integer.MAX_VALUE) {
                        return (int) L;
                    } else {
                        return L;
                    }
                }
            }
            case START_ARRAY: {
                List<Object> arr = new ArrayList<>();
                for (JsonToken t = r.next(); t != JsonToken.END_ARRAY; t = r.next()) arr.add(readAny(r));
                return arr;
            }
            case START_OBJECT: {
                Map<String, Object> map = new LinkedHashMap<>();
                for (JsonToken t = r.next(); t != JsonToken.END_OBJECT; t = r.next()) {
                    if (t != JsonToken.FIELD_NAME && t != JsonToken.VALUE_STRING) throw error("期望字段名");
                    String k = r.string();
                    r.next();
                    map.put(k, readAny(r));
                }
                return map;
            }
            default:
                throw error("未知 token: " + r.token());
        }
    }

    static boolean isPlatformClass(Class<?> c) {
        if (c == null) return false;
        if (c.getClassLoader() == null) return true;
        String n = c.getName();
        return n.startsWith("java.")
                || n.startsWith("javax.")
                || n.startsWith("jdk.")
                || n.startsWith("sun.")
                || n.startsWith("com.sun.");
    }
}
