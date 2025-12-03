package xyz.ytora.ytool.id.support;

import xyz.ytora.ytool.id.IdGenerator;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地内存自增 ID 生成器（Long）
 *
 * 特性：
 * - 线程安全（AtomicLong，无锁）
 * - 支持起始值、步长、最大值
 * - 支持三种溢出策略：抛异常 / 环回 / 阻塞等待
 *
 * 注意：
 * - 仅在【单进程/单实例】内保证严格递增与唯一；
 * - 应用重启后会从起始值重新开始（如需跨重启单调，请结合持久化存储起点或使用号段模式）。
 */
public final class LocalIdGenerator implements IdGenerator<Long> {

    /** 溢出后的处理策略 */
    public enum OverflowPolicy {
        /** 触达 maxValue 后抛出异常 */
        THROW,
        /** 环回到 startValue 继续（不再全局唯一，只在区间内循环） */
        WRAP,
        /** 阻塞等待，直到有“空间”（通常不推荐，除非外部会推进 maxValue） */
        BLOCK
    }

    private final long startValue;           // 起始值（包含）
    private final long step;                 // 步长（>0）
    private final long maxValue;             // 上限值（包含）
    private final OverflowPolicy policy;     // 溢出策略
    private final long blockSleepNanos;      // BLOCK 时的休眠间隔

    private final AtomicLong counter;        // 当前计数（下一个将要返回的值）

    /**
     * 常用构造：从 1 开始，步长 1，上限 Long.MAX_VALUE，溢出抛异常
     */
    public LocalIdGenerator() {
        this(1L, 1L, Long.MAX_VALUE, OverflowPolicy.THROW, 1, TimeUnit.MILLISECONDS);
    }

    /**
     * 完整构造
     *
     * @param startValue      起始值（包含）
     * @param step            步长，必须 > 0
     * @param maxValue        上限值（包含），必须 >= startValue
     * @param policy          溢出策略
     * @param blockSleepStep  policy=BLOCK 时的睡眠时长
     * @param unit            时间单位
     */
    public LocalIdGenerator(long startValue,
                            long step,
                            long maxValue,
                            OverflowPolicy policy,
                            long blockSleepStep,
                            TimeUnit unit) {
        if (step <= 0) throw new IllegalArgumentException("step must be > 0");
        if (maxValue < startValue) throw new IllegalArgumentException("maxValue must be >= startValue");
        if (unit == null) unit = TimeUnit.MILLISECONDS;
        if (policy == null) policy = OverflowPolicy.THROW;

        this.startValue = startValue;
        this.step = step;
        this.maxValue = maxValue;
        this.policy = policy;
        this.blockSleepNanos = unit.toNanos(Math.max(1, blockSleepStep));
        this.counter = new AtomicLong(startValue);
    }

    /**
     * 获取下一个递增 ID（线程安全）
     */
    @Override
    public Long nextId() {
        while (true) {
            long current = counter.get();
            // 超出上限，按策略处理
            if (current > maxValue) {
                switch (policy) {
                    case THROW:
                        throw new IllegalStateException("ID overflow: current=" + current + ", max=" + maxValue);
                    case WRAP:
                        // 环回到起始值：尝试把计数器重置为 startValue
                        if (counter.compareAndSet(current, startValue)) {
                            // 成功后返回 startValue，并让下次从 startValue + step 继续
                            return startValue;
                        }
                        // 失败则重试
                        continue;
                    case BLOCK:
                        // 阻塞等待，直到外部把 maxValue 提高
                        try {
                            TimeUnit.NANOSECONDS.sleep(blockSleepNanos);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted while blocking on overflow", ie);
                        }
                        continue;
                }
            }

            long next = current + step;
            // CAS 设置下一个起点：保证无锁并发安全
            if (counter.compareAndSet(current, next)) {
                return current;
            }
            // CAS 失败则自旋重试
        }
    }

    // 便捷构造：自定义起始值/步长，溢出抛异常
    public static LocalIdGenerator of(long startValue, long step) {
        return new LocalIdGenerator(startValue, step, Long.MAX_VALUE,
                OverflowPolicy.THROW, 1, TimeUnit.MILLISECONDS);
    }

    // 只读配置（方便调试/监控）
    @Override
    public String toString() {
        return "InMemorySequentialIdGenerator{" +
                "startValue=" + startValue +
                ", step=" + step +
                ", maxValue=" + maxValue +
                ", policy=" + policy +
                '}';
    }
}

