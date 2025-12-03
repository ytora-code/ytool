package xyz.ytora.ytool.convert;

/**
 * 异常类
 */
public class ConverterException extends RuntimeException {
    private final static int code = 1;
    private final String message;

    public ConverterException(String msg) {
        super(msg);
        this.message = msg;
    }

    public ConverterException(String msg, Throwable cause) {
        super(msg, cause);
        this.message = msg;
    }

    public ConverterException(Throwable cause) {
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
