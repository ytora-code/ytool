package xyz.ytora.ytool.json.config.convert;

/**
 * created by yangtong on 2025/8/18 20:19:04
 * <br/>
 */

import xyz.ytora.ytool.json.context.JsonReadContext;
import xyz.ytora.ytool.json.context.JsonWriteContext;
import xyz.ytora.ytool.json.reader.JsonReader;

import java.lang.reflect.Type;

/**
 * 类型转换器：可拦截序列化/反序列化，支持泛型（通过 declaredType 获取真实参数）
 */
public interface JsonTypeConverter<T> {
    /**
     * 反序列化：从 JsonReader 当前 token 开始消费，返回目标对象
     */
    T read(JsonReader r, Type declaredType, JsonReadContext ctx);

    /**
     * 序列化：把 value 以 JSON 写入 out（可用 ctx 递归写子值）
     */
    void write(StringBuilder out, T value, Type declaredType, JsonWriteContext ctx);
}
