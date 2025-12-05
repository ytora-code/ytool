package xyz.ytora.ytool.json;

import xyz.ytora.ytool.str.Strs;

/**
 * JSON异常类
 */
public class BaseException extends RuntimeException {
    private Integer code;
    private String message;
    //原始异常类型
    private Throwable throwable;

    public BaseException(String message, Object... params) {
        this.message = Strs.format(message, params);
    }

    public BaseException(Throwable throwable, String message, Object... params) {
        this.message = Strs.format(message, params);
    }

    public BaseException(Throwable cause) {
        super(cause.getMessage(), cause);
        this.message = cause.getMessage();
        this.throwable = cause;
    }
}
