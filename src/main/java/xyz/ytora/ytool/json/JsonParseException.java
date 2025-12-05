package xyz.ytora.ytool.json;

import xyz.ytora.ytool.str.Strs;

/**
 * created by yang tong on 2025/8/18 16:18:38
 * <br/>
 * JSON解析时的异常
 */
public class JsonParseException extends RuntimeException {
    private Integer code = 10;
    private String message;

    public JsonParseException(String msg) {
        super(msg);
    }

    public JsonParseException(Throwable throwable, String message, Object... params) {
        this.message = Strs.format(message, params);
    }
}
