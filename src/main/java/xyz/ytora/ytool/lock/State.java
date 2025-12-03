package xyz.ytora.ytool.lock;

/**
 * created by yangtong on 2021/6/7 下午12:31
 * <p>
 * 用于在多个线程间维护单个共享int变量
 * try(Mutex _ = state.withMutex()) {
 * //该块里面的代码就是同步代码块
 * } catch(Exception e){}
 */
public final class State {
    private final Mutex mutex = new Mutex();
    private int state;

    public State() {
        this(0);
    }

    public State(int initialState) {
        this.state = initialState;
    }

    /**
     * 为了保证下面所有的操作都是原子性的，需要先调用该方法加锁
     *
     * @return Mutex对象
     */
    public Mutex withMutex() {
        return mutex.acquire();
    }

    /**
     * 跟回state的值
     *
     * @return state值
     */
    public int get() {
        return state;
    }

    /**
     * 直接设置state的值
     *
     * @param state 新值
     */
    public void set(int state) {
        this.state = state;
    }

    /**
     * 注册
     * a |= b 等价于 a = a | b，将左边的变量与右边的值进行按位或运算，然后将结果赋值给左边的变量
     * a = 1010, b = 0110 那么a |= b --> a = 1110
     *
     * @param mask 用于设置的标志位
     */
    public void register(int mask) {
        this.state |= mask;
    }

    /**
     * 反注册
     * a &= b 等价于 a = a & b
     * 比如a = 1110，b = 0110
     * 则~b = 1001，那么a &= ~b --> a = 1000
     *
     * @param mask 用于清除的标志位
     * @return 返回是否清除成功
     */
    public boolean unregister(int mask) {
        boolean r = (state & mask) > 0;
        state &= ~mask;
        return r;
    }

    /**
     * 通过cas方式设置state的值
     * 调用该方法时，确保处于try(Mutex _ = state.withMutex())块的内部，以保证原子性
     *
     * @param expectedValue 期待的旧值
     * @param newValue      设置的新值
     * @return 是否设置成功
     */
    public boolean cas(int expectedValue, int newValue) {
        if (state == expectedValue) {
            state = newValue;
            return true;
        } else {
            return false;
        }
    }
}