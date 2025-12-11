package xyz.ytora.ytool.json;

import xyz.ytora.ytool.convert.Converts;

import java.util.HashMap;

/**
 * JSON 对象
 */
public class JSON extends HashMap<String, Object> {

    public JSON(Integer size) {
        super(size);
    }

    /**
     * 获取 Integer 值
     */
    public Integer getInteger(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer intValue) {
            return intValue;
        }
        // 尝试类型转换
        return Converts.convert(value, Integer.class);
    }

    /**
     * 获取 String 值
     */
    public String getString(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String strValue) {
            return strValue;
        }
        // 尝试类型转换
        return Converts.convert(value, String.class);
    }

    /**
     * 获取 Boolean 值
     */
    public Boolean getBoolean(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        // 尝试类型转换
        return Converts.convert(value, Boolean.class);
    }

    /**
     * 获取 Double 值
     */
    public Double getDouble(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Double doubleValue) {
            return doubleValue;
        }
        // 尝试类型转换
        return Converts.convert(value, Double.class);
    }


    /**
     * 获取 JSON 值
     */
    public JSON getJSON(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof JSON jsonValue) {
            return jsonValue;
        }
        throw new JsonParseException("key为 " + key + " 的数据无法转为 JSON 对象");
    }

    /**
     * 获取 JSON 数组
     */
    public JSON[] getJSONArray(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof JSON[] jsonArray) {
            return jsonArray;
        }
        throw new JsonParseException("key为 " + key + " 的数据无法转为 JSON 数组");
    }

}
