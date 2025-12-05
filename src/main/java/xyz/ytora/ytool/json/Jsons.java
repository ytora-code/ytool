package xyz.ytora.ytool.json;

import xyz.ytora.ytool.bean.Beans;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * created by yangtong on 2025/4/13 21:05:49
 * <br/>
 * 全局 JSON 工具（基于 UltraJson + JsonMapper）。
 * - fromJsonStr / toJsonStr 与旧版保持同名
 * - 提供 TypeRef（兼容 Jackson TypeReference 用法）
 */
public final class Jsons {

    /**
     * 全局唯一的 JsonMapper
     */
    private static volatile JsonMapper JSON_MAPPER = JsonMapper.createDefault();

    // 预置的常用 TypeRef
    public static final TypeRef<Map<String, String>> MAP_OF_STRING_STRING = new TypeRef<>() {
    };
    public static final TypeRef<Map<String, Object>> MAP_OF_STRING_OBJECT = new TypeRef<>() {
    };

    private Jsons() {
    }

    /**
     * 注册自定义 JsonMapper（例如：注册全局 TypeAdapter、命名策略等）
     */
    public static void register(JsonMapper mapper) {
        JSON_MAPPER = mapper;
    }

    // ----------------------- 反序列化 -----------------------

    public static <T> T fromJsonStr(String jsonStr, Class<T> type) {
        return JSON_MAPPER.fromJson(jsonStr, type);
    }

    public static <T> T fromJsonStr(String jsonStr, TypeRef<T> typeRef) {
        return JSON_MAPPER.fromJson(jsonStr, typeRef);
    }

    // ----------------------- 序列化 -----------------------

    public static <T> String toJsonStr(T obj) {
        return JSON_MAPPER.toJson(obj);
    }

    // ----------------------- Map 互转 -----------------------

    /** 对象 → Map<String,Object> */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj.getClass().isArray() || Collection.class.isAssignableFrom(obj.getClass()) || Beans.isPrimitiveWrapper(obj)) {
            throw new IllegalArgumentException("目标对象不是可映射为 JSON 对象的类型: " + obj.getClass());
        }
        try {
            if (obj instanceof String str) {
                return JSON_MAPPER.fromJson(str, MAP_OF_STRING_OBJECT);
            } else if (obj instanceof Map<?, ?> m) {
                // 尽量返回 <String,Object> 视图（保持顺序）
                LinkedHashMap<String, Object> out = new LinkedHashMap<>(m.size());
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
                return out;
            }

            // 对于 POJO：toDom 后应为 Map 结构
            String json = JSON_MAPPER.toJson(obj);
            return JSON_MAPPER.fromJson(json, MAP_OF_STRING_OBJECT);
        } catch (RuntimeException e) {
            throw wrap(e, "对象转Map失败: {}", obj.getClass().getName());
        }
    }

    /**
     * Map<String,Object> → 对象（反向构建）
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        try {
            String json = JSON_MAPPER.toJson(map);
            return JSON_MAPPER.fromJson(json, clazz);
        } catch (RuntimeException e) {
            throw wrap(e, "Map转对象失败，目标类型: {}", clazz);
        }
    }

    // ----------------------- 快捷别名 -----------------------

    /**
     * JSON 字符串 → Map<String,Object>
     */
    public static Map<String, Object> fromJsonStrToMap(String jsonStr) {
        return fromJsonStr(jsonStr, MAP_OF_STRING_OBJECT);
    }

    /**
     * JSON 字符串 → Map<String,String>
     */
    public static Map<String, String> fromJsonStrToStringMap(String jsonStr) {
        return fromJsonStr(jsonStr, MAP_OF_STRING_STRING);
    }

    // ----------------------- 内部工具 -----------------------

    private static BaseException wrap(Throwable cause, String msg, Object arg) {
        // 包装为 BaseException；保留原始异常栈
        return new BaseException(cause, msg, arg);
    }
}
