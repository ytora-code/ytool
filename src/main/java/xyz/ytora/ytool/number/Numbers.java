package xyz.ytora.ytool.number;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * created by yangtong on 2025/7/13 00:22:19
 * <br/>
 * 数字工具类
 */
public class Numbers {

    /** 默认除法运算精度 */
    private static final int DEFAULT_SCALE = 10;

    /** 默认舍入模式 */
    private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * 除法
     * @param number1 被除数
     * @param number2 除数
     * @param scale 精度
     * @param mode 舍入模式
     * @return number1 除以 number2 的值
     */
    public static BigDecimal div(Number number1, Number number2, int scale, RoundingMode mode) {
        BigDecimal bd1 = toBigDecimal(number1);
        BigDecimal bd2 = toBigDecimal(number2);

        if (bd2.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("除数不能为0");
        }
        return bd1.divide(bd2, scale, mode);
    }

    /**
     * 除法
     * @param number1 被除数
     * @param number2 除数
     * @param scale 精度
     * @return number1 除以 number2 的值
     */
    public static BigDecimal div(Number number1, Number number2, int scale) {
        return div(number1, number2, scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 除法
     * @param number1 被除数
     * @param number2 除数
     * @param mode 舍入模式
     * @return number1 除以 number2 的值
     */
    public static BigDecimal div(Number number1, Number number2, RoundingMode mode) {
        return div(number1, number2, DEFAULT_SCALE, mode);
    }

    /**
     * 除法
     * @param number1 被除数
     * @param number2 除数
     * @return number1 除以 number2 的值
     */
    public static BigDecimal div(Number number1, Number number2) {
        return div(number1, number2, DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 加法
     * @param number1 加数1
     * @param number2 加数2
     * @return 两数之和
     */
    public static BigDecimal add(Number number1, Number number2) {
        return toBigDecimal(number1).add(toBigDecimal(number2));
    }

    /**
     * 减法
     * @param number1 被减数
     * @param number2 减数
     * @return 两数之差
     */
    public static BigDecimal sub(Number number1, Number number2) {
        return toBigDecimal(number1).subtract(toBigDecimal(number2));
    }

    /**
     * 乘法
     * @param number1 乘数1
     * @param number2 乘数2
     * @return 两数之积
     */
    public static BigDecimal mul(Number number1, Number number2) {
        return toBigDecimal(number1).multiply(toBigDecimal(number2));
    }

    /**
     * 比较
     * @param number1 数字1
     * @param number2 数字2
     * @return 如果number1 > number2返回1，相等返回0，小于返回-1
     */
    public static int compare(Number number1, Number number2) {
        return toBigDecimal(number1).compareTo(toBigDecimal(number2));
    }

    /**
     * 四舍五入（指定舍入模式）
     * @param number 数字
     * @param scale 小数位数
     * @param mode 舍入模式
     * @return 四舍五入后的结果
     */
    public static BigDecimal round(Number number, int scale, RoundingMode mode) {
        return toBigDecimal(number).setScale(scale, mode);
    }

    /**
     * 取绝对值
     * @param number 数字
     * @return 绝对值
     */
    public static BigDecimal abs(Number number) {
        return toBigDecimal(number).abs();
    }

    /**
     * 将字符串转换为对应类型的 Number（Integer、Long、Double）
     * @param input 输入字符串
     * @return 对应的 Number 实例，非法输入返回 null
     */
    public static Number toNumber(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        input = input.trim();

        try {
            if (input.contains(".")) {
                // 如果包含小数点，尝试解析为 Double
                return Double.parseDouble(input);
            } else {
                // 尝试先解析为 Integer，再尝试 Long
                try {
                    return Integer.parseInt(input);
                } catch (NumberFormatException e1) {
                    return Long.parseLong(input);
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将Number类型转换为BigDecimal
     * @param number 数字
     * @return BigDecimal对象
     */
    private static BigDecimal toBigDecimal(Number number) {
        if (number == null) {
            throw new IllegalArgumentException("数字不能为null");
        }

        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        } else if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        } else if (number instanceof Double || number instanceof Float) {
            // 对于浮点数，使用字符串转换避免精度问题
            return new BigDecimal(number.toString());
        } else {
            // 对于整数类型（Integer, Long, Short, Byte等）
            return BigDecimal.valueOf(number.longValue());
        }
    }

    /**
     * 判断type是否属于基本类型
     */
    public static Boolean isPrimitive(Class<?> type) {
        return type == int.class || type == long.class || type == double.class ||
                type == float.class || type == short.class || type == byte.class;
    }

}
