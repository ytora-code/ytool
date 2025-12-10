package xyz.ytora.ytool.coll;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 集合工具类
 */
public class Colls {

    /**
     * 经过条件筛选后，获取容器内唯一一个元素
     */
    public static <T> Optional<T> one(Collection<T> collection, Predicate<T> predicate) {
        List<T> candidateItem = collection.stream().filter(predicate).toList();
        if (candidateItem.size() > 1) {
            throw new CollectionException("期待容器集合" + collection + "只有一个元素，但是实际发现了" + candidateItem.size() + "个");
        } else if (candidateItem.size() == 1) {
            return Optional.of(candidateItem.get(0));
        }
        return Optional.empty();
    }

    /**
     * 判断容器是否为空
     */
    public static <T> Boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断容器是否为空
     */
    public static <T> Boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    /**
     * 计算两个集合的交集
     * @param list1 第一个集合
     * @param list2 第二个集合
     * @param <T> 元素类型
     * @return 交集结果
     */
    public static <T> List<T> intersection(List<T> list1, List<T> list2) {
        if (list1 == null || list2 == null) {
            return new ArrayList<>();
        }

        Set<T> set1 = new HashSet<>(list1);
        Set<T> set2 = new HashSet<>(list2);
        set1.retainAll(set2);
        return new ArrayList<>(set1);
    }

    /**
     * 计算两个集合的并集
     * @param list1 第一个集合
     * @param list2 第二个集合
     * @param <T> 元素类型
     * @return 并集结果
     */
    public static <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> unionSet = new HashSet<>();
        if (list1 != null) {
            unionSet.addAll(list1);
        }
        if (list2 != null) {
            unionSet.addAll(list2);
        }
        return new ArrayList<>(unionSet);
    }

    /**
     * 计算两个集合的补集 (list1 - list2)
     * @param list1 第一个集合
     * @param list2 第二个集合
     * @param <T> 元素类型
     * @return 补集结果 (list1中存在但list2中不存在的元素)
     */
    public static <T> List<T> diff(List<T> list1, List<T> list2) {
        if (list1 == null) {
            return new ArrayList<>();
        }
        if (list2 == null) {
            return new ArrayList<>(list1);
        }

        Set<T> set1 = new HashSet<>(list1);
        Set<T> set2 = new HashSet<>(list2);
        set1.removeAll(set2);
        return new ArrayList<>(set1);
    }

    /**
     * 计算两个集合的对称差集 (并集 - 交集)
     * @param list1 第一个集合
     * @param list2 第二个集合
     * @param <T> 元素类型
     * @return 对称差集结果
     */
    public static <T> List<T> symmetricDiff(List<T> list1, List<T> list2) {
        List<T> diff1 = diff(list1, list2);
        List<T> diff2 = diff(list2, list1);
        return union(diff1, diff2);
    }

    /**
     * 判断两个集合是否有交集
     */
    public static <T> boolean hasIntersection(Collection<T> c1, Collection<T> c2) {
        if (isEmpty(c1) || isEmpty(c2)) {
            return false;
        }
        return c1.stream().anyMatch(c2::contains);
    }

    /**
     * 判断集合1是否是集合2的子集
     */
    public static <T> boolean isSubset(Collection<T> subset, Collection<T> superset) {
        if (isEmpty(subset)) {
            return true;
        }
        if (isEmpty(superset)) {
            return false;
        }
        return superset.containsAll(subset);
    }

    /**
     * 判断两个集合是否相等（忽略顺序）
     */
    public static <T> boolean isEqual(Collection<T> c1, Collection<T> c2) {
        if (c1 == c2) return true;
        if (c1 == null || c2 == null) return false;
        return new HashSet<>(c1).equals(new HashSet<>(c2));
    }

    // ========== 集合转换方法 ==========

    /**
     * 去重
     */
    public static <T> List<T> distinct(List<T> list) {
        if (isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 集合转换为另一种类型
     */
    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * 过滤集合
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * 分组
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> classifier) {
        if (isEmpty(collection)) {
            return new HashMap<>();
        }
        return collection.stream().collect(Collectors.groupingBy(classifier));
    }

    // ========== 集合操作方法 ==========

    /**
     * 安全获取集合大小
     */
    public static <T> int size(Collection<T> collection) {
        return collection == null ? 0 : collection.size();
    }

    /**
     * 获取第一个元素
     */
    public static <T> Optional<T> first(List<T> list) {
        if (isEmpty(list)) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.get(0));
    }

    /**
     * 获取最后一个元素
     */
    public static <T> Optional<T> last(List<T> list) {
        if (isEmpty(list)) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.get(list.size() - 1));
    }

    /**
     * 安全获取指定索引的元素
     */
    public static <T> Optional<T> get(List<T> list, int index) {
        if (isEmpty(list) || index < 0 || index >= list.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.get(index));
    }

    /**
     * 反转集合
     */
    public static <T> List<T> reverse(List<T> list) {
        if (isEmpty(list)) {
            return new ArrayList<>();
        }
        List<T> reversed = new ArrayList<>(list);
        Collections.reverse(reversed);
        return reversed;
    }

    /**
     * 随机打乱集合
     */
    public static <T> List<T> shuffle(List<T> list) {
        if (isEmpty(list)) {
            return new ArrayList<>();
        }
        List<T> shuffled = new ArrayList<>(list);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    /**
     * 分页获取
     */
    public static <T> List<T> page(List<T> list, int pageNum, int pageSize) {
        if (isEmpty(list) || pageNum < 1 || pageSize < 1) {
            return new ArrayList<>();
        }

        int startIndex = (pageNum - 1) * pageSize;
        if (startIndex >= list.size()) {
            return new ArrayList<>();
        }

        int endIndex = Math.min(startIndex + pageSize, list.size());
        return list.subList(startIndex, endIndex);
    }

    /**
     * 分批处理
     */
    public static <T> List<List<T>> partition(List<T> list, int batchSize) {
        if (isEmpty(list) || batchSize < 1) {
            return new ArrayList<>();
        }

        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    // ========== 集合创建方法 ==========

    /**
     * 创建不可变列表
     */
    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        if (elements == null || elements.length == 0) {
            return Collections.emptyList();
        }
        return List.of(elements);
    }

    /**
     * 创建不可变集合
     */
    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        if (elements == null || elements.length == 0) {
            return Collections.emptySet();
        }
        return Set.of(elements);
    }

    /**
     * 重复元素创建列表
     */
    public static <T> List<T> repeat(T element, int count) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        return Collections.nCopies(count, element);
    }

    /**
     * 数字范围创建列表
     */
    public static List<Integer> range(int start, int end) {
        return range(start, end, 1);
    }

    public static List<Integer> range(int start, int end, int step) {
        List<Integer> list = new ArrayList<>();
        if (step > 0) {
            for (int i = start; i < end; i += step) {
                list.add(i);
            }
        } else if (step < 0) {
            for (int i = start; i > end; i += step) {
                list.add(i);
            }
        }
        return list;
    }

    // ========== 统计分析方法 ==========

    /**
     * 统计元素出现次数
     */
    public static <T> Map<T, Long> frequency(Collection<T> collection) {
        if (isEmpty(collection)) {
            return new HashMap<>();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));
    }

    /**
     * 找出最常出现的元素
     */
    public static <T> Optional<T> mostFrequent(Collection<T> collection) {
        if (isEmpty(collection)) {
            return Optional.empty();
        }
        return frequency(collection).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /**
     * 找出最少出现的元素
     */
    public static <T> Optional<T> leastFrequent(Collection<T> collection) {
        if (isEmpty(collection)) {
            return Optional.empty();
        }
        return frequency(collection).entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    // ========== 数值集合专用方法 ==========

    /**
     * 求和（Integer）
     */
    public static int sum(Collection<Integer> numbers) {
        if (isEmpty(numbers)) {
            return 0;
        }
        return numbers.stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 求和（Double）
     */
    public static double sumDouble(Collection<Double> numbers) {
        if (isEmpty(numbers)) {
            return 0.0;
        }
        return numbers.stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * 平均值
     */
    public static OptionalDouble average(Collection<? extends Number> numbers) {
        if (isEmpty(numbers)) {
            return OptionalDouble.empty();
        }
        return numbers.stream().mapToDouble(Number::doubleValue).average();
    }

    /**
     * 最大值
     */
    public static <T extends Comparable<T>> Optional<T> max(Collection<T> collection) {
        if (isEmpty(collection)) {
            return Optional.empty();
        }
        return collection.stream().max(Comparable::compareTo);
    }

    /**
     * 最小值
     */
    public static <T extends Comparable<T>> Optional<T> min(Collection<T> collection) {
        if (isEmpty(collection)) {
            return Optional.empty();
        }
        return collection.stream().min(Comparable::compareTo);
    }

    /**
     * 将数组转为List
     */
    public static List<Object> arrToList(Object array) {
        if (array instanceof Object[] objArray) {
            return Arrays.asList(objArray);
        }
        int length = Array.getLength(array);
        List<Object> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(Array.get(array, i));
        }
        return list;
    }
}
