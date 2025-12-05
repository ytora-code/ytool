package xyz.ytora.ytool.date;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * CRON 表达式工具类
 */
public class Crons {
    // cron表达式解析器
    private static CronParser parser;

    /**
     * 根据cron表达式计算相对于baseTimeMillis的下一次的时间
     * @param cronExpr cron表达式
     * @param baseTimeMillis 基准时间
     * @return 下一次执行时间的毫秒戳
     */
    public static Long nextTimeByCron(String cronExpr, long baseTimeMillis) {
        if (cronExpr == null) {
            throw new NullPointerException("不能为空cronExpr");
        }
        if (parser == null) {
            parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        }
        Cron cron = parser.parse(cronExpr);
        cron.validate();

        ExecutionTime executionTime = ExecutionTime.forCron(cron);

        ZonedDateTime baseTime = Instant.ofEpochMilli(baseTimeMillis).atZone(ZoneId.systemDefault());
        Optional<ZonedDateTime> nextTime = executionTime.nextExecution(baseTime);
        return nextTime.map(zdt -> zdt.toInstant().toEpochMilli()).orElse(-1L);
    }

    public static Long nextTimeByCron(String cronExpr) {
        return nextTimeByCron(cronExpr, System.currentTimeMillis());
    }
}
