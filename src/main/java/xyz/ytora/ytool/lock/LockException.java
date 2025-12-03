package xyz.ytora.ytool.lock;

/**
 * 异常类
 */
public class LockException extends RuntimeException {
    private final static int code = 1;
    private final String message;

    public LockException(String msg) {
        super(msg);
        this.message = msg;
    }

    public LockException(String msg, Throwable cause) {
        super(msg, cause);
        this.message = msg;
    }

    public LockException(Throwable cause) {
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
