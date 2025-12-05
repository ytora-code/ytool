package xyz.ytora.ytool.json.reader.source;

/**
 * created by YT on 2025/8/22 11:04:29
 * <br/>
 * 基于byte[]
 */
public final class Utf8ByteArraySource implements CharSource {
    private final byte[] bytes;
    private int boff, blen;
    private final char[] cbuf;
    private int cpos, clen;

    // 保留 UTF-8 跨界未完成前缀的状态
    Utf8ByteArraySource(byte[] b, int off, int len, int charBufSize) {
        this.bytes = b;
        this.boff = off;
        this.blen = off + len;
        this.cbuf = new char[charBufSize];
        refill();
    }

    private void refill() {
        // 从 bytes[boff..blen) 解码填充 cbuf；处理 UTF-8 跨界与非法序列
        // 更新 cpos=0, clen=填充后的字符数；更新 boff 到新位置
    }

    public boolean hasNext() {
        return cpos < clen || boff < blen;
    }

    public char next() {
        if (cpos >= clen) refill();
        return cbuf[cpos++];
    }

    public char peek() {
        if (cpos >= clen) refill();
        return cpos < clen ? cbuf[cpos] : (char) -1;
    }

    public void back1() {
        cpos--;
    }

    public int position() { /* 维护一个累加的已产出字符计数 */
        return 0;
    }

    public int line() {
        return 0;
    }

    public int column() {
        return 0;
    }
}
