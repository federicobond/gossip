package ar.edu.itba.it.gossip.util;

import org.apache.commons.lang3.Validate;

public abstract class Validations {
    public static void require(boolean condition, String errorMessageTemplate,
            Object... errorMessageArgs) {
        Validate.isTrue(condition, errorMessageTemplate, errorMessageArgs);
    }

    public static void assumeState(boolean condition,
            String errorMessageTemplate, Object... errorMessageArgs) {
        Validate.validState(condition, errorMessageTemplate, errorMessageArgs);
    }

    public static void assumeNotSet(Object object, String errorMessageTemplate,
            Object... errorMessageArgs) {
        Validate.validState(object == null, errorMessageTemplate, object,
                errorMessageArgs);
    }
}
