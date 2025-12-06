package xyz.ytora.ytool.io;

import xyz.ytora.ytool.str.Strs;

/**
 * IO异常
 */
public class IosException extends RuntimeException {
    private Integer code;
    private String message;
    //原始异常类型
    private Throwable throwable;

    public IosException(String message, Object... params) {
        this.message = Strs.format(message, params);
    }

    public IosException(Throwable throwable, String message, Object... params) {
        this.message = Strs.format(message, params);
    }

    public IosException(Throwable cause) {
        super(cause.getMessage(), cause);
        this.message = cause.getMessage();
        this.throwable = cause;
    }
}
