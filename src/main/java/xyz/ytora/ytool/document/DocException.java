package xyz.ytora.ytool.document;

import xyz.ytora.ytool.str.Strs;

/**
 * 文档异常
 */
public class DocException extends RuntimeException {
    private Integer code;
    private String message;
    //原始异常类型
    private Throwable throwable;

    public DocException(String message, Object... params) {
        this.message = Strs.format(message, params);
    }

    public DocException(Throwable throwable, String message, Object... params) {
        this.message = Strs.format(message, params);
    }

    public DocException(Throwable cause) {
        super(cause.getMessage(), cause);
        this.message = cause.getMessage();
        this.throwable = cause;
    }
}
