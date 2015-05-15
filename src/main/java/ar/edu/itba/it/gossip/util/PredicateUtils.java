package ar.edu.itba.it.gossip.util;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.function.Predicate;

public abstract class PredicateUtils {
    public static Predicate<?> isInstanceOf(Class<?> clazz) {
        return x -> clazz.isInstance(x);
    }

    @SafeVarargs
    public static <V> Predicate<V> isInstanceOfAny(
            Class<? extends V>... classes) {
        return isInstanceOfAny(asList(classes));
    }

    public static <V> Predicate<V> isInstanceOfAny(
            List<Class<? extends V>> classes) {
        return x -> classes.stream().anyMatch(clazz -> clazz.isInstance(x));
    }

    public static <V> Predicate<? super Class<?>> hasInstance(V x) {
        return clazz -> clazz.isInstance(x);
    }
}
