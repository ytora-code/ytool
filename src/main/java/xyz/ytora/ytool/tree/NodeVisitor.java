package xyz.ytora.ytool.tree;

/**
 * 树的节点处理回调
 */
@FunctionalInterface
public interface NodeVisitor<T> {
    /**
     * @param level  当前层级（根=0）
     * @param node   当前节点
     * @param parent 父节点（根节点为 null）
     */
    void accept(int level, T node, T parent);
}
