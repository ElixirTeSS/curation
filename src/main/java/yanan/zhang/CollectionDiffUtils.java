package yanan.zhang;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Diff tools
 *
 * @author Yanan Zhang
 **/
public class CollectionDiffUtils {

    private CollectionDiffUtils() {

    }

    /**
     * compare two set
     *
     * @param newList   new data set
     * @param existList existing data set
     * @param beanKey   key
     * @param match     new data can match the existing data
     * @param notMatch  new data cannot match the existing data
     * @param <T>
     * @param <K>
     */
    public static <T, K> void compare(Collection<T> newList, Collection<T> existList, Function<T, K> beanKey, BiConsumer<T, T> match, Consumer<T> notMatch) {
        if (CollectionUtils.isEmpty(newList)) {
            return;
        }

        Map<K, T> twoMap = existList.stream().collect(Collectors.toMap(beanKey, x -> x));

        for (T t1 : newList) {
            K k1 = beanKey.apply(t1);
            T existsBean = twoMap.get(k1);
            if (existsBean != null) {
                match.accept(t1, existsBean);
            } else {
                notMatch.accept(t1);
            }
        }
    }

    /**
     * compare two set: get the data which need to be inserted, deleted or updated
     *
     * @param newList   new data set
     * @param existList existing data set
     * @param beanKey   key
     * @param <T>
     * @param <K>
     * @return
     */
    public static <T, K> CollectionDiffResult<T> diff(Collection<T> newList, Collection<T> existList, Function<T, K> beanKey) {
        return diff(newList, existList, beanKey, (newBean, existsBean) -> {
        });
    }

    /**
     * compare two set: get the data which need to be inserted, deleted or updated
     *
     * @param newList      new data set
     * @param existList    existing data set
     * @param beanKey      key
     * @param beforeUpdate 1st param: newBean, 2nd param: existBean
     * @param <T>
     * @param <K>
     * @return
     */
    public static <T, K> CollectionDiffResult<T> diff(Collection<T> newList, Collection<T> existList, Function<T, K> beanKey, BiConsumer<T, T> beforeUpdate) {
        CollectionDiffResult<T> diffResult = new CollectionDiffResult<>();

        compare(newList, existList, beanKey, (newBean, existBean) -> {
            if (!newBean.equals(existBean)) {
                beforeUpdate.accept(newBean, existBean);
                diffResult.addUpdate(newBean);
            }
        }, diffResult::addInsert);
        compare(existList, newList, beanKey, (existsBean, newBean) -> {
        }, diffResult::addDelete);

        return diffResult;
    }

    /**
     * compare two set: get the data which need to be inserted, deleted or updated
     * data need to be updated: insert first, then delete
     *
     * @param newList   new data set
     * @param existList existing data set
     * @param beanKey   key
     * @param <T>
     * @param <K>
     * @return
     */
    public static <T, K> CollectionDiffResult<T> diffIgnoreUpdate(Collection<T> newList, Collection<T> existList, Function<T, K> beanKey) {
        CollectionDiffResult<T> diffResult = new CollectionDiffResult<>();

        compare(newList, existList, beanKey, (newBean, existsBean) -> {
            if (!newBean.equals(existsBean)) {
                diffResult.addInsert(newBean);
                diffResult.addDelete(existsBean);
            }
        }, diffResult::addInsert);
        compare(existList, newList, beanKey, (existsBean, newBean) -> {
        }, diffResult::addDelete);

        return diffResult;
    }

    /**
     * compare two lists (same element)
     *
     * @param list
     * @param equalsFunc check two elements are equal
     * @param adaptFunc
     * @param <T>
     * @param <R>
     * @return organised by same element
     */
    @SuppressWarnings("unchecked")
    public static <T, R> List<Set<R>> group(List<T> list, BiFunction<T, T, Boolean> equalsFunc, Function<T, R> adaptFunc) {
        List<Set<R>> returnList = new ArrayList<>(list.size());
        Set<T> calculatedItems = new HashSet<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            T current = list.get(i);

            Set<T> group = Sets.newHashSet(current);
            for (int j = 0; j < list.size(); j++) {
                if (i == j) {
                    continue;
                }
                T compare = list.get(j);
                boolean isSameGroup = equalsFunc.apply(current, compare);
                for (T g : group) {
                    isSameGroup = isSameGroup && equalsFunc.apply(g, compare);
                }
                if (isSameGroup) {
                    group.add(compare);
                    calculatedItems.add(compare);
                }
            }

            if (calculatedItems.contains(current) && group.size() == 1) {
                continue;
            }
            returnList.add(group.stream().map(adaptFunc).collect(Collectors.toSet()));
        }

        return returnList.stream().distinct().collect(Collectors.toList());
    }

    public static void main(String[] args) {

        List<Bean> newList = Lists.newArrayList(new Bean(1, "a"), new Bean(2, "b"));
        List<Bean> existList = Lists.newArrayList(new Bean(2, "c"), new Bean(3, "c"));
        CollectionDiffResult<Bean> diffResult = diff(newList, existList, Bean::getId);
        System.out.println(diffResult);
        System.out.println(diffResult.getChangedList());
        System.out.println(diffIgnoreUpdate(newList, existList, Bean::getId));

    }

    @ToString
    static class Bean {

        private static final long serialVersionUID = -5632590512202721334L;

        private int id;

        private String name;

        public Bean(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Bean bean = (Bean) o;
            return id == bean.id &&
                    Objects.equals(name, bean.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }
}