package xyz.ytora.ytool.date;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

/**
 * created by yangtong on 2025/4/4 下午3:50
 * <br/>
 * 关于java.util.Date的工具类
 */
public class Dates {

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    // 私有构造方法，防止工具类被实例化
    private Dates() {
        throw new RuntimeException("不能构建Dates类");
    }

    /**
     * 将自定义格式的字符串构建为 Date 对象
     */
    public static Date parse(String dateStr, String formatStr) {
        SimpleDateFormat sdf = new SimpleDateFormat(formatStr);
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            // 如果需要可以在这里记录错误日志
            throw new RuntimeException(e);
        }
    }

    /**
     * 将字符串yyyy-MM-dd格式的字符串构建为 Date 对象
     */
    public static Date parseDate(String dateStr) {
        return parse(dateStr, DATE_PATTERN);
    }

    /**
     * 将字符串yyyy-MM-dd HH:mm:ss格式的字符串构建为 Date 对象
     */
    public static Date parseDateTime(String dateStr) {
        return parse(dateStr, DATETIME_PATTERN);
    }

    /**
     * 将 Date 对象格式化为指定格式的字符串
     */
    public static String format(Date date, String formatStr) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(formatStr);
        return sdf.format(date);
    }

    /**
     * 格式化 Date 对象为字符串（格式："yyyy-MM-dd"）
     */
    public static String formatDate(Date date) {
        return format(date, DATE_PATTERN);
    }

    /**
     * 格式化 Date 对象为字符串（格式："yyyy-MM-dd HH:mm:ss"）
     */
    public static String formatDateTime(Date date) {
        return format(date, DATETIME_PATTERN);
    }

    /**
     * 获取今天的日期字符串
     */
    public static String today() {
        return formatDate(new Date());
    }

    /**
     * 获取当前时间的日期字符串
     */
    public static String now() {
        return formatDateTime(new Date());
    }

    /**
     * 通用的日期偏移方法
     *
     * @param date   原始日期
     * @param unit  偏移单位
     * @param limit 偏移量(正数表示向后，负数表示向前)
     * @return 偏移后的日期
     */
    public static Date offset(Date date, DateUnit unit, int limit) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        switch (unit) {
            case MONTH -> calendar.add(Calendar.MONTH, limit);
            case YEAR -> calendar.add(Calendar.YEAR, limit);
            default -> calendar.setTimeInMillis(calendar.getTimeInMillis() + unit.getMillis() * limit);
        }
        return calendar.getTime();
    }
}
