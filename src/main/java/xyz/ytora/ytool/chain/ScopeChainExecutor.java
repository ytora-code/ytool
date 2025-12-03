package xyz.ytora.ytool.chain;

import java.util.List;

/**
 * created by yangtong on 2025/7/16 14:18:01
 * <br/>
 */
public class ScopeChainExecutor {

    public static void execute(List<ScopeInvocation> invocations, Runnable task) {
        // 构建执行链
        Runnable chain = buildChain(invocations, 0, task);
        // 启动执行
        chain.run();
    }

    private static Runnable buildChain(List<ScopeInvocation> invocations, int index, Runnable finalTask) {
        if (index >= invocations.size()) {
            return finalTask;
        }

        return () -> {
            ScopeInvocation current = invocations.get(index);
            Runnable next = buildChain(invocations, index + 1, finalTask);
            current.invoke(next);
        };
    }

    public static void main(String[] args) {
        List<ScopeInvocation> invocations = List.of(
                next -> {
                    System.out.println("Scope 1: start");
                    next.run();  // 显式调用下一个
                    System.out.println("Scope 1: end");
                },
                next -> {
                    System.out.println("Scope 2: start");
                    next.run();  // 显式调用下一个
                    System.out.println("Scope 2: end");
                }
        );

        ScopeChainExecutor.execute(invocations, () -> {
            System.out.println("Main task executed");
        });
    }
}
