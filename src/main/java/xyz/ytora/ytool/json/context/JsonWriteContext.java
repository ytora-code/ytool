package xyz.ytora.ytool.json.context;

import java.lang.reflect.Type;

/**
 * created by yangtong on 2025/8/18 20:20:09
 * <br/>
 */
public interface JsonWriteContext {
    void writeValue(StringBuilder out, Object value, Type declaredType);
}
