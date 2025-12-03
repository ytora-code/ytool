package xyz.ytora.ytool.str;

/**
 * created by yangtong on 2025/4/4 下午6:04
 * <br/>
 * 占位符
 */
public enum Placeholder {
    LEFT_PARENTHESIS("("),
    RIGHT_PARENTHESIS(")"),
    LEFT_CURLY_BRACE("{"),
    LEFT_CURLY_BRACE_PLUS("#{"),
    LEFT_CURLY_BRACE_DOLLAR("${"),
    RIGHT_CURLY_BRACE("}"),
    LEFT_BRACKET("["),
    RIGHT_BRACKET("]"),
    ;

    public final String placeholder;
    Placeholder(String placeholder) {
        this.placeholder = placeholder;
    }
}
