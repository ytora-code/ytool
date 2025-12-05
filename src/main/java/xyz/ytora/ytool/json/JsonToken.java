package xyz.ytora.ytool.json;

/**
 * created by yang tong on 2025/8/18 16:15:51
 * <br/>
 * JSON TOKEN
 */
public enum JsonToken {
    /**
     * 对象开始 {
     */
    START_OBJECT,
    /**
     * 对象开始 }
     */
    END_OBJECT,
    /**
     * 数组结束 [
     */
    START_ARRAY,
    /**
     * 数组结束 ]
     */
    END_ARRAY,
    /**
     * 字段
     */
    FIELD_NAME,
    /**
     * 字符串值
     */
    VALUE_STRING,
    /**
     * 数值
     */
    VALUE_NUMBER,
    /**
     * 布尔值
     */
    VALUE_BOOLEAN,
    /**
     * NULL 值
     */
    VALUE_NULL,
    /**
     * 结束
     */
    EOF
}
