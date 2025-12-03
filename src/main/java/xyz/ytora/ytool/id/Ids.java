package xyz.ytora.ytool.id;

import xyz.ytora.ytool.id.support.LocalIdGenerator;
import xyz.ytora.ytool.id.support.SnowflakeId;
import xyz.ytora.ytool.id.support.ULID;

/**
 * created by yangtong on 2025/8/9 21:51:05
 * <br/>
 * ID 生成工具类
 */
public class Ids {

    /**
     * 使用雪花算法产生ID
     */
    private static final IdGenerator<Long> snowflakeId = new SnowflakeId(0, 0);

    /**
     * 使用ULID产生ID
     */
    private static final IdGenerator<String> ulid = new ULID();

    /**
     * 基于内存的本地ID产生器，中心化
     */
    private static final IdGenerator<Long> localId = new LocalIdGenerator();


    public static Long snowflakeId() {
        return snowflakeId.nextId();
    }

    public static String ulid() {
        return ulid.nextId();
    }

    public static Long localId() {
        return localId.nextId();
    }

}
