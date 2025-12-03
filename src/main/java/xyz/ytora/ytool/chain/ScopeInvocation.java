package xyz.ytora.ytool.chain;

/**
 * created by yangtong on 2025/7/16 14:16:15
 * <br/>
 */
public interface ScopeInvocation {
    void invoke(Runnable next);
}
