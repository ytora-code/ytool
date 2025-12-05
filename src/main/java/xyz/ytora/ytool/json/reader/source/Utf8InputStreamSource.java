package xyz.ytora.ytool.json.reader.source;

import java.io.InputStream;

/**
 * created by YT on 2025/8/22 11:05:01
 * <br/>
 * 基于流，支持超大 JSON；内部维护 byte[] 与 char[] 双缓冲
 */
public class Utf8InputStreamSource implements CharSource, AutoCloseable {
    private final InputStream in;
    private final byte[] bbuf;
    private int bi, bj;
    private final char[] cbuf;
    private int ci, cj;

    // 保留 UTF-8 部分字节状态 + 行列计数
    Utf8InputStreamSource(InputStream in, int byteBufSize, int charBufSize) {
        this.in = in;
        this.bbuf = new byte[byteBufSize];
        this.cbuf = new char[charBufSize];
        refill();
    }

    private void refill() {
        // 若 char 用尽则从流读入 bbuf，再进行 UTF-8 解码到 cbuf；
        // 处理跨块 UTF-8 序列；到达 EOF 时标记 hasNext()=false
    }

    public boolean hasNext() {
        return ci < cj;
    }

    public char next() {
        if (ci >= cj) refill();
        return ci < cj ? cbuf[ci++] : (char) -1;
    }

    public char peek() {
        if (ci >= cj) refill();
        return ci < cj ? cbuf[ci] : (char) -1;
    }

    public void back1() {
        ci--;
    }

    public int position() {
        return /* 已产出字符计数 */ 0;
    }

    public int line() {
        return 0;
    }

    public int column() {
        return 0;
    }

    public void close() {
        try {
            in.close();
        } catch (Exception ignore) {
        }
    }
}
