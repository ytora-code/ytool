package xyz.ytora.ytool.json.reader;

import xyz.ytora.ytool.json.JsonParseException;
import xyz.ytora.ytool.json.JsonToken;

/**
 * created by yang tong on 2025/8/18 16:19:28
 * <br/>
 * 将JSON字符串读取成token
 */
public final class JsonReader {
    private final char[] buf;
    // 当前指针
    private int cur;
    // 字符串最大长度
    private final int length;
    // 当前token
    private JsonToken token = null;

    // 临时值
    private String str;
    private long longVal;
    private double doubleVal;
    private boolean numIsDouble;
    private boolean boolVal;

    // 是否允许尾逗号：允许 [1,2,] / {"a":1,}
    private final boolean allowTrailingComma;

    // 在对象里读到 key 之后，下一次 next() 直接读取 value
    private boolean pendingValueAfterField = false;

    public JsonReader(String json) {
        // 默认宽松，允许尾随逗号
        this(json, true);
    }

    public JsonReader(String json, boolean allowTrailingComma) {
        this.buf = json.toCharArray();
        this.length = buf.length;
        this.cur = 0;
        this.allowTrailingComma = allowTrailingComma;
    }

    public JsonToken token() {
        return token;
    }

    public String string() {
        return str;
    }

    public long longVal() {
        return longVal;
    }

    public double doubleVal() {
        return doubleVal;
    }

    public boolean isDoubleNumber() {
        return numIsDouble;
    }

    public boolean boolVal() {
        return boolVal;
    }

    public JsonToken next() {
        if (pendingValueAfterField) {
            pendingValueAfterField = false;
            // 跳过key和其对应value之间的所有空白字符和逗号（可选）
            skipWsAndOptionalCommas();
            readValueToken();
            return token;
        }

        skipWsAndOptionalCommas();
        if (cur >= length) {
            token = JsonToken.EOF;
            return token;
        }

        char c = buf[cur++];

        switch (c) {
            case '{':
                token = JsonToken.START_OBJECT;
                return token;
            case '}':
                token = JsonToken.END_OBJECT;
                return token;
            case '[':
                token = JsonToken.START_ARRAY;
                return token;
            case ']':
                token = JsonToken.END_ARRAY;
                return token;

            case ':':
                // 宽松处理：直接跳过
                return next();

            case ',':
                // 宽松处理：允许多余逗号，继续读下一个
                return next();

            case '"': {
                str = readString();
                skipWhitespace();
                // 判断是否 FIELD_NAME（对象上下文才会出现 ':'，但我们做宽松判断）
                if (cur < length && buf[cur] == ':') {
                    cur++; // 吃掉 ':'
                    token = JsonToken.FIELD_NAME;
                    pendingValueAfterField = true;
                } else {
                    token = JsonToken.VALUE_STRING;
                }
                return token;
            }

            case 't': // true
            case 'f': // false
                cur--;
                readBoolean();
                token = JsonToken.VALUE_BOOLEAN;
                return token;

            case 'n': // null
                cur--;
                readNull();
                token = JsonToken.VALUE_NULL;
                return token;

            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    cur--;
                    readNumber();
                    token = JsonToken.VALUE_NUMBER;
                    return token;
                }
                if (isWs(c)) {
                    // 宽松：空白当没看见
                    return next();
                }
                throw error("非法字符: " + printable(c));
        }
    }

    /**
     * 跳过空白字符和逗号
     */
    private void skipWsAndOptionalCommas() {
        while (cur < length) {
            char c = buf[cur];
            if (isWs(c)) {
                cur++;
                continue;
            }
            if (c == ',' && allowTrailingComma) {
                // 宽松：全局跳过多余逗号（含尾随逗号）
                cur++;
                continue;
            }
            break;
        }
    }

    private void skipWhitespace() {
        while (cur < length && isWs(buf[cur])) cur++;
    }

    private boolean isWs(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private String readString() {
        // 前置：已消费起始双引号
        StringBuilder sb = new StringBuilder(16);
        while (cur < length) {
            char c = buf[cur++];
            if (c == '"') return sb.toString();

            if (c == '\\') {
                if (cur >= length) throw error("字符串转义不完整");
                char e = buf[cur++];
                switch (e) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u': {
                        int cp1 = readUnicode4();
                        if (isHighSurrogate(cp1)) {
                            // 期待 \uDC00.. \uDFFF
                            if (cur + 1 < length && buf[cur] == '\\' && buf[cur + 1] == 'u') {
                                // 吃掉 '\ u'
                                cur += 2;
                                int cp2 = readUnicode4();
                                if (!isLowSurrogate(cp2)) {
                                    // 不是合法的低代理；宽松：把第一个按 BMP 写入，再把第二个也按 BMP 写
                                    appendCodePoint(sb, cp1);
                                    appendCodePoint(sb, cp2);
                                } else {
                                    int codePoint = toCodePoint(cp1, cp2);
                                    appendCodePoint(sb, codePoint);
                                }
                            } else {
                                // 没有后续低代理；宽松：写入高代理对应字符
                                appendCodePoint(sb, cp1);
                            }
                        } else {
                            appendCodePoint(sb, cp1);
                        }
                        break;
                    }
                    default:
                        throw error("未知转义: \\" + e);
                }
            } else {
                if (c < 0x20) {
                    // JSON 规范要求：字符串内禁止未转义控制字符
                    throw error("字符串包含未转义控制字符 U+" + Integer.toHexString(c));
                }
                sb.append(c);
            }
        }
        throw error("字符串未闭合");
    }

    private int readUnicode4() {
        if (cur + 4 > length) throw error("unicode 转义不完整");
        int code = hex(buf[cur]) << 12 | hex(buf[cur + 1]) << 8 | hex(buf[cur + 2]) << 4 | hex(buf[cur + 3]);
        cur += 4;
        return code;
    }

    private int hex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
        if (c >= 'A' && c <= 'F') return 10 + (c - 'A');
        throw error("非法十六进制字符: " + c);
    }

    private static boolean isHighSurrogate(int cp) {
        return cp >= 0xD800 && cp <= 0xDBFF;
    }

    private static boolean isLowSurrogate(int cp) {
        return cp >= 0xDC00 && cp <= 0xDFFF;
    }

    private static int toCodePoint(int hi, int lo) {
        return ((hi - 0xD800) << 10) + (lo - 0xDC00) + 0x10000;
    }

    private static void appendCodePoint(StringBuilder sb, int codePoint) {
        if (codePoint <= 0xFFFF) sb.append((char) codePoint);
        else sb.append(Character.toChars(codePoint));
    }

    private void readBoolean() {
        if (matchKeyword("true")) {
            boolVal = true;
            return;
        }
        if (matchKeyword("false")) {
            boolVal = false;
            return;
        }
        throw error("非法布尔字面量");
    }

    private void readNull() {
        if (!matchKeyword("null")) throw error("非法 null 字面量");
    }

    private boolean matchKeyword(String kw) {
        int m = kw.length();
        if (cur + m > length) return false;
        for (int k = 0; k < m; k++) {
            if (buf[cur + k] != kw.charAt(k)) return false;
        }
        cur += m;
        return true;
    }

    private void readNumber() {
        int start = cur;
        boolean hasDot = false;
        boolean hasExp = false;

        // 符号
        if (cur < length && (buf[cur] == '-' || buf[cur] == '+')) cur++;

        // 整数部分
        if (cur < length && buf[cur] == '0') {
            cur++;
            // 严格的话：后面不能紧跟数字；宽松模式我们不强制
        } else {
            if (cur >= length || !isDigit(buf[cur])) throw error("数字格式错误");
            while (cur < length && isDigit(buf[cur])) cur++;
        }

        // 小数
        if (cur < length && buf[cur] == '.') {
            hasDot = true;
            cur++;
            if (cur >= length || !isDigit(buf[cur])) throw error("小数点后缺少数字");
            while (cur < length && isDigit(buf[cur])) cur++;
        }

        // 指数
        if (cur < length && (buf[cur] == 'e' || buf[cur] == 'E')) {
            hasExp = true;
            cur++;
            if (cur < length && (buf[cur] == '+' || buf[cur] == '-')) cur++;
            if (cur >= length || !isDigit(buf[cur])) throw error("指数部分缺少数字");
            while (cur < length && isDigit(buf[cur])) cur++;
        }

        String num = new String(buf, start, cur - start);
        try {
            if (hasDot || hasExp) {
                numIsDouble = true;
                doubleVal = Double.parseDouble(num);
            } else {
                numIsDouble = false;
                longVal = Long.parseLong(num);
            }
        } catch (NumberFormatException e) {
            throw error("数字解析失败: " + num);
        }
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void readValueToken() {
        if (cur >= length) throw error("缺少值");
        char c = buf[cur++];
        switch (c) {
            case '"':
                str = readString();
                token = JsonToken.VALUE_STRING;
                return;
            case '{':
                token = JsonToken.START_OBJECT;
                return;
            case '[':
                token = JsonToken.START_ARRAY;
                return;
            case 't':
            case 'f':
                cur--;
                readBoolean();
                token = JsonToken.VALUE_BOOLEAN;
                return;
            case 'n':
                cur--;
                readNull();
                token = JsonToken.VALUE_NULL;
                return;
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    cur--;
                    readNumber();
                    token = JsonToken.VALUE_NUMBER;
                    return;
                }
                if (isWs(c) || (allowTrailingComma && c == ',')) {
                    // 宽松：允许 value 前有空白/逗号
                    cur--;
                    skipWsAndOptionalCommas();
                    readValueToken();
                    return;
                }
                throw error("值起始非法字符: " + printable(c));
        }
    }

    private JsonParseException error(String msg) {
        return new JsonParseException(msg + " @ pos " + cur);
    }

    private static String printable(char c) {
        if (c < 32) return String.format("\\u%04x", (int) c);
        return "'" + c + "'";
    }
}

