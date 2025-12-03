package xyz.ytora.ytool.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * created by yangtong on 2021/6/7 下午12:28
 * 封装了ReentrantLock锁，配合try-with-resources进行自动释放
 */
public record Mutex(Lock lock) implements AutoCloseable {
    public Mutex {
        if (lock == null) {
            throw new LockException("lock不能为空");
        }
    }

    public Mutex() {
        this(new ReentrantLock());
    }

    public Mutex acquire() {
        lock.lock();
        return this;
    }

    @Override
    public void close() {
        lock.unlock();
    }
}
