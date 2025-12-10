package xyz.ytora.ytool.coll;

import xyz.ytora.ytool.str.Strs;

/**
 * 集合异常
 */
public class CollectionException extends RuntimeException {
    private Integer code = 10;
    private String message;

    public CollectionException(String msg) {
        super(msg);
    }

    public CollectionException(Throwable throwable, String message, Object... params) {
        this.message = Strs.format(message, params);
    }
}
