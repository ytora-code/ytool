package xyz.ytora.ytool.id.support;

import xyz.ytora.ytool.id.IdGenerator;

/**
 * 雪花算法（Snowflake）ID 生成器
 * 结构（默认 Twitter 经典版 41-5-5-12）：
 *  ┌────────────── 1 bit 符号位（恒为0） ──────────────┐
 *  │                                                │
 *  │ 41 bit 时间戳（毫秒，偏移自定义纪元）                │
 *  │ 5 bit  数据中心ID（datacenterId）                 │
 *  │ 5 bit  工作节点ID（workerId）                     │
 *  │ 12 bit 自增序列号（同一毫秒内从0递增，溢出则等到下一毫秒）│
 *  └──────────────────────────────────────────────── ┘
 * 主要特性：
 *  - 单机并发：同毫秒最多 4096（2^12）个 ID
 *  - 近似按时间递增（同毫秒内为序列递增）
 *  - 无中心化依赖，适合作为数据库 BIGINT 主键
 * 回拨策略：
 *  - 如果检测到当前系统时间 < 上次生成时间（即时钟回拨）
 *    - 若回拨差值 <= backwardToleranceMs（默认5ms），则自旋等待到 lastTimestamp 再继续
 *    - 若回拨差值 > 容忍阈值，则抛出异常，交由上层监控/熔断/降级
 */
public final class SnowflakeId implements IdGenerator<Long> {

    /** 默认纪元：2025-08-10 00:00:00 UTC 的毫秒值 */
    public static final long DEFAULT_EPOCH = 1754755200000L;

    // 位宽定义（可改为构造参数，这里用 Twitter 经典值）
    private static final long WORKER_ID_BITS = 3L;   // 工作节点 0~8
    private static final long DATACENTER_ID_BITS = 3L;   // 数据中心 0~8
    private static final long SEQUENCE_BITS = 10L;  // 自增序列 0~1023

    // 可取最大值（位运算生成全1掩码）
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);      // 8
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);  // 8
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);       // 1023

    // 各段左移位数
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                                 // 12
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;                // 17
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS; // 22

    // 配置
    private final long epoch;                 // 自定义纪元
    private final long datacenterId;          // 数据中心ID [0,31]
    private final long workerId;              // 工作节点ID [0,31]
    private final long backwardToleranceMs;   // 时钟回拨容忍毫秒数（建议 1~10ms）

    // 运行时状态
    private long lastTimestamp = -1L;         // 上一次生成ID的时间戳（毫秒）
    private long sequence = 0L;               // 当前毫秒内的序列号

    /**
     * 构造方法（使用默认纪元与 5ms 回拨容忍）
     */
    public SnowflakeId(long datacenterId, long workerId) {
        this(DEFAULT_EPOCH, datacenterId, workerId, 5L);
    }

    /**
     * 完整构造方法
     * @param epoch                自定义纪元（毫秒）。建议固定后不再变更
     * @param datacenterId         数据中心ID，范围 [0, 31]
     * @param workerId             工作节点ID，范围 [0, 31]
     * @param backwardToleranceMs  时钟回拨容忍阈值（毫秒），超过则抛异常
     */
    public SnowflakeId(long epoch, long datacenterId, long workerId, long backwardToleranceMs) {
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("datacenterId out of range [0," + MAX_DATACENTER_ID + "]");
        }
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId out of range [0," + MAX_WORKER_ID + "]");
        }
        if (epoch < 0) {
            throw new IllegalArgumentException("epoch must be >= 0");
        }
        if (backwardToleranceMs < 0) {
            throw new IllegalArgumentException("backwardToleranceMs must be >= 0");
        }
        this.epoch = epoch;
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        this.backwardToleranceMs = backwardToleranceMs;
    }

    /**
     * 生成下一个全局唯一 ID（线程安全）
     * 说明：
     *  - synchronized 保证并发安全（简单可靠）。如需更高并发，可用 LongAdder+CAS 优化，但复杂度更高。
     *  - 同毫秒内自增序列；溢出则等待到下一毫秒。
     */
    public synchronized Long nextId() {
        long currentTs = timeGen();

        // 检测时钟回拨
        if (currentTs < lastTimestamp) {
            long offset = lastTimestamp - currentTs;
            if (offset <= backwardToleranceMs) {
                // 小回拨：等待到 lastTimestamp 再继续
                currentTs = waitUntil(lastTimestamp);
            } else {
                // 大回拨：直接失败，交由上层处理（例如报警/熔断/切换时钟源）
                throw new IllegalStateException(
                        "Clock moved backwards. Refusing to generate id for " + offset + " ms");
            }
        }

        if (currentTs == lastTimestamp) {
            // 同一毫秒内，自增序列并取低位掩码
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 当前毫秒序列耗尽，等待到下一毫秒
                currentTs = waitUntil(lastTimestamp + 1);
            }
        } else {
            // 跨毫秒，序列重置为0
            sequence = 0L;
        }

        lastTimestamp = currentTs;

        // 组装ID（各字段左移后按位或）
        long timePart = (currentTs - epoch) << TIMESTAMP_LEFT_SHIFT;
        long dataPart = (datacenterId << DATACENTER_ID_SHIFT);
        long workPart = (workerId << WORKER_ID_SHIFT);
        return timePart | dataPart | workPart | sequence;
    }

    /**
     * 从生成的ID中反解析出：原始毫秒时间戳（系统时间）
     */
    public long extractTimestamp(long id) {
        long timePart = (id >>> TIMESTAMP_LEFT_SHIFT);
        return timePart + epoch;
    }

    /**
     * 从生成的ID中反解析出：数据中心ID
     */
    public long extractDatacenterId(long id) {
        return (id >>> DATACENTER_ID_SHIFT) & MAX_DATACENTER_ID;
    }

    /**
     * 从生成的ID中反解析出：工作节点ID
     */
    public long extractWorkerId(long id) {
        return (id >>> WORKER_ID_SHIFT) & MAX_WORKER_ID;
    }

    /**
     * 从生成的ID中反解析出：序列号
     */
    public long extractSequence(long id) {
        return id & SEQUENCE_MASK;
    }

    /** 当前毫秒时间（可替换为单调时钟封装，便于测试/注入） */
    public long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 自旋等待，直到时间 >= targetMs
     * 注意：此处是忙等，通常等待极短（<1ms）。如需更省CPU可加入 Thread.yield() 或 sleep(0)。
     */
    private long waitUntil(long targetMs) {
        long ts = timeGen();
        while (ts < targetMs) {
            ts = timeGen();
        }
        return ts;
    }

    // 便捷查看配置
    @Override
    public String toString() {
        return "SnowflakeId{" +
                "epoch=" + epoch +
                ", datacenterId=" + datacenterId +
                ", workerId=" + workerId +
                ", backwardToleranceMs=" + backwardToleranceMs +
                '}';
    }
}
