package ar.edu.itba.it.gossip.util;

import java.util.function.Consumer;

import org.apache.log4j.Logger;

public interface Logging {
    // as per Logger.getLogger() this method DOES NOT return a new logger on
    // each call
    default Logger getLogger() {
        return Logger.getLogger(getClass());
    }

    default void logInfo(String message) {
        getLogger().info(message);
    }

    default void logDebug(String message) {
        getLogger().debug(message);
    }

    default void logError(String message) {
        getLogger().error(message);
    }

    default Consumer<Throwable> onErrorLog(String message) {
        return exception -> getLogger().error("error starting listener",
                exception);
    }
}
