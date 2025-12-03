package xyz.ytora.ytool.date;

/**
 * created by yangtong on 2025/4/4 下午4:11
 * <br/>时间单位，以毫秒为基准
 * <br/>Java 中，每个枚举常量本质上就是一个 public static final 的对象实例，只会被创建一次
 */
public enum DateUnit {
    //毫秒
    MS(1L),
    //秒，等于1毫秒 * 1000
    SECOND(MS.getMillis() * 1000L),
    //分钟，等于1秒 * 60
    MINUTE(SECOND.getMillis() * 60L),
    //小时，等于1分钟 * 60
    HOUR(MINUTE.getMillis() * 60L),
    //天，等于1小时 * 24
    DAY(HOUR.getMillis() * 24L),
    //周，等于1天 * 7
    WEEK(DAY.getMillis() * 7L),
    //月，每月的时间长度不固定，因此使用-1
    MONTH(-1),
    //年，每年的时间长度不固定，因此使用-1
    YEAR(-1);

    private final long millis;

    DateUnit(long millis) {
        this.millis = millis;
    }

    public long getMillis() {
        if (millis < 0) {
            throw new RuntimeException("无法获取固定的毫秒数");
        }
        return this.millis;
    }
}
