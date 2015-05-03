package ar.edu.itba.it.gossip.util;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

public abstract class ObjectUtils {
    public static int hash(Object... objects) {
        return Objects.hash(objects);
    }

    public static boolean areEqual(Object a, Object b) {
        return Objects.equals(a, b);
    }

    public static ToStringBuilder toStringBuilder(Object obj) {
        return new ToStringBuilder(obj);
    }
}
