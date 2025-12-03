package xyz.ytora.ytool.tree;

import java.util.List;

/**
 * 树形数据规范
 * 要求，
 */
public interface ITree<T> {
    /**
     * 获取当前数据id
     */
    String getId();

    /**
     * 获取当前数据父id，如果一个元素没有父元素，则pid = 0
     */
    String getPid();

    /**
     * 获取当前数据的关键字
     */
    String getKey();

    /**
     * 获取当前数据的所有子数据
     */
    List<T> getChildren();

    /**
     * 设置当前数据的所有子数据
     */
    void setChildren(List<T> children);

    /**
     * 设置当前数据是否有子数据
     */
    void hasChildren(Boolean hasChildren);
}
