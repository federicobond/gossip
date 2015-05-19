package ar.edu.itba.it.gossip.util;

import static java.util.Arrays.stream;

import java.util.function.Predicate;

public abstract class PredicateUtils {
    public static <V> Predicate<V> isInstanceOf(Class<? extends V> clazz) {
        return x -> clazz.isInstance(x);
    }

    @SafeVarargs
    public static <V> Predicate<V> isInstanceOfAny(
            Class<? extends V>... classes) {
        return x -> stream(classes).anyMatch(clazz -> clazz.isInstance(x));
    }
}
