package xyz.ytora.ytool.json.reader.source;

/**
 * created by YT on 2025/8/22 10:56:00
 * <br/>
 * JSON来源，可能是字符串，char[]，byte[]或者流
 */
public interface CharSource {

    boolean hasNext();

    char next();        // 读取并前进

    char peek();        // 窥视但不前进；若无数据需尝试填充

    void back1();       // 回退 1 个 char（保证只回退一次）

    int position();     // 绝对字符偏移（用于 @ pos 报错）

    int line();         // 可选：行号（从 1 开始）

    int column();       // 可选：列号（从 1 开始）

    // 可覆写
    default void close() {
    }

}
