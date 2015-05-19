package ar.edu.itba.it.gossip.util;

import static ar.edu.itba.it.gossip.util.ValidationUtils.require;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

public abstract class CollectionUtils {
    public static <K, V> Pair<K, V> pair(K key, V value) {
        return Pair.of(key, value);
    }

    public static <K, V> Map<K, V> mapBy(Stream<V> values, Function<V, K> f) {
        Map<K, V> map = new HashMap<>();
        values.forEach(value -> {
            K key = f.apply(value);
            require(!map.containsKey(key),
                    "Function %s generates repeated keys for %s (key=%s)", f,
                    values, key);
            map.put(key, value);
        });
        return unmodifiableMap(map);
    }

    @SafeVarargs
    public static <K, V> Map<K, V> asMap(Pair<K, V>... pairs) {
        return asMap(asList(pairs));
    }

    public static <K, V> Map<K, V> asMap(Collection<Pair<K, V>> pairs) {
        Map<K, V> map = new HashMap<>();
        for (Pair<K, V> pair : pairs) {
            K key = pair.getKey();
            require(!map.containsKey(key), "Repeated key: %s", key);
            map.put(pair.getKey(), pair.getValue());
        }
        return unmodifiableMap(map);
    }

    public static String[] subarray(List<String> list, int from, int to) {
        return list.subList(from, to).toArray(new String[0]);
    }

    public static String[] subarray(List<String> list, int from) {
        return subarray(list, from, list.size());
    }

    public static <K, V> Pair<List<K>, List<V>> unzip(
            List<Pair<K, V>> zippedList) {
        List<K> keys = new ArrayList<K>(zippedList.size());
        List<V> values = new ArrayList<V>(zippedList.size());

        for (Pair<K, V> zippedPair : zippedList) {
            keys.add(zippedPair.getKey());
            values.add(zippedPair.getValue());
        }

        return Pair.of(keys, values);
    }

    public static <K, V> boolean contentsAreEqual(Collection<Pair<K, V>> pairs,
            Map<K, V> map) {
        return contentsAreEqual(map, pairs);
    }

    public static <K, V> boolean contentsAreEqual(Map<K, V> map,
            Collection<Pair<K, V>> pairs) {
        return contentsAreEqual(map, asMap(pairs));
    }

    public static <K, V> boolean contentsAreEqual(Map<K, V> map1, Map<K, V> map2) {
        if (map1.size() != map2.size()) {
            return false;
        }

        Set<Entry<K, V>> entries1 = map1.entrySet();
        Set<Entry<K, V>> entries2 = map2.entrySet();

        return entries1.containsAll(entries2) && entries2.containsAll(entries1);
    }

    public static <K, V> boolean containsAll(Map<K, V> map,
            Collection<Pair<K, V>> pairs) {
        if (map.size() < pairs.size()) {
            return false;
        }
        for (Pair<K, V> pair : pairs) {
            if (!contains(map, pair)) {
                return false;
            }
        }
        return true;
    }

    public static <K, V> boolean contains(Map<K, V> map, Pair<K, V> pair) {
        V value = map.get(pair.getKey());
        return value != null && value.equals(pair.getValue());
    }

    public static <K, V> boolean contains(Map<K, V> map, Entry<K, V> entry) {
        V value = map.get(entry.getKey());
        return value != null && value.equals(entry.getValue());
    }

    @SafeVarargs
    public static <V> V last(V... values) {
        require(values.length > 0, "There are no elements in the array");
        return values[values.length - 1];
    }

    public static <V> V last(List<V> values) {
        require(values.size() > 0, "There are no elements in the list");
        return values.get(values.size() - 1);
    }
}
