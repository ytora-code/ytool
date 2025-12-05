package xyz.ytora.ytool.json.context;

import xyz.ytora.ytool.json.reader.JsonReader;

import java.lang.reflect.Type;

/**
 * created by yangtong on 2025/8/18 20:19:26
 * <br/>
 */
public interface JsonReadContext {
    Object readValue(Type type, JsonReader r);
}
