package xyz.ytora.ytool.json.reader.source;

/**
 * created by YT on 2025/8/22 11:03:49
 * <br/>
 * 基于char[]
 */
public final class CharArraySource implements CharSource {
    private final char[] buf;
    private int cur;
    private int line = 1, col = 1;

    CharArraySource(char[] buf) {
        this.buf = buf;
    }

    public boolean hasNext() {
        return cur < buf.length;
    }

    public char next() {
        char c = buf[cur++];
        if (c == '\n') {
            line++;
            col = 1;
        } else col++;
        return c;
    }

    public char peek() {
        return hasNext() ? buf[cur] : (char) -1;
    }

    public void back1() {
        cur--;
    }

    public int position() {
        return cur;
    }

    public int line() {
        return line;
    }

    public int column() {
        return col;
    }
}
