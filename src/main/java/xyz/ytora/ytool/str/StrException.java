package xyz.ytora.ytool.str;

/**
 * 异常类
 */
public class StrException extends RuntimeException {
    private final static int code = 1;
    private final String message;

    public StrException(String msg) {
        super(msg);
        this.message = msg;
    }

    public StrException(String msg, Throwable cause) {
        super(msg, cause);
        this.message = msg;
    }

    public StrException(Throwable cause) {
        super(cause.getMessage(), cause);
        this.message = cause.getMessage();
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static int getCode() {
        return code;
    }
}
