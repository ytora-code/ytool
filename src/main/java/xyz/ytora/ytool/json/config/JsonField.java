package xyz.ytora.ytool.json.config;

/**
 * created by yangtong on 2025/8/19 11:08:17
 * <br/>
 * JSON字段
 */
public @interface JsonField {

    String value() default "";

    /**
     * 该字段是否必须
     */
    boolean required() default false;

    /**
     * 是否忽略该字段
     */
    boolean ignore() default false;

}
