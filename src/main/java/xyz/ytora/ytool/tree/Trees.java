package xyz.ytora.ytool.tree;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 树状数据工具类
 */
public class Trees {

    // private static final Logger log = LoggerFactory.getLogger(Trees.class);

    /**
     * 将目标数据变成具有层级的数组结构
     * @param items 目标数据
     * @return 树状数据
     * @param <T> 目标数据元素类型
     */
    public static <T extends ITree<T>> List<T> toTree(List<T> items) {
        //将list映射成map
        Map<String, T> nodeMap = items.stream().collect(Collectors.toMap(ITree::getId, Function.identity()));
        //存储所有顶层节点
        List<T> roots = new ArrayList<>();

        //给每一个元素找父亲
        for (T item : items) {
            //如果是根节点，直接加入roots
            if ("0".equals(item.getPid()) || item.getPid() == null) {
                roots.add(item);
            }
            //非根节点，则寻找其父节点并进行挂载
            else {
                T parent = nodeMap.get(item.getPid());
                //父节点存在，直接挂载
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(item);

                    parent.hasChildren(true);
                }
                //如果父节点不存在，则忽略
                else {
                    //roots.add(item);
                    // log.warn("节点【{}】未找到父节点【{}】,将被忽略", item, item.getPid());
                }
            }
        }

        return roots;
    }

    /**
     * 根据关键字key搜索树
     * @param items 目标数据
     * @param key 关键字
     * @return 树状数据
     * @param <T> 目标数据元素类型
     */
    public static <T extends ITree<T>> List<T> toTree(List<T> items, String key) {
        if (key == null) {
            return toTree(items);
        }

        Map<String, T> nodeMap = items.stream().collect(Collectors.toMap(ITree::getId, Function.identity()));

        //找到所有匹配的节点
        Set<T> matchedNodes = items.stream()
                .filter(item -> item.getKey() != null && item.getKey().contains(key))
                .collect(Collectors.toSet());

        //回溯添加匹配节点的所有父节点
        Set<T> resultNodes = new HashSet<>(matchedNodes);
        for (T matchedNode : matchedNodes) {
            T current = matchedNode;
            while (current.getPid() != null) {
                T parent = nodeMap.get(current.getPid());
                if (parent == null || resultNodes.contains(parent)) {
                    break;
                }
                resultNodes.add(parent);
                current = parent;

                parent.hasChildren(true);
            }
        }

        return toTree(new ArrayList<>(resultNodes));
    }
}
