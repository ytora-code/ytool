package xyz.ytora.ytool.id.support;

import xyz.ytora.ytool.id.IdGenerator;

import java.util.UUID;

/**
 * UUID 生成器
 */
public class UuidGenerator implements IdGenerator<String> {
    @Override
    public String nextId() {
        return UUID.randomUUID().toString();
    }
}
