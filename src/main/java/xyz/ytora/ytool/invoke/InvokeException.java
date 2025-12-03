package xyz.ytora.ytool.invoke;

/**
 * 异常类
 */
public class InvokeException extends RuntimeException {
    private final static int code = 1;
    private final String message;

    public InvokeException(String msg) {
        super(msg);
        this.message = msg;
    }

    public InvokeException(String msg, Throwable cause) {
        super(msg, cause);
        this.message = msg;
    }

    public InvokeException(Throwable cause) {
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
