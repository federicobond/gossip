package ar.edu.itba.it.gossip.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ExceptionUtils {
    /**
     * For internal use mostly. It allows to 'add' error handling to regular
     * dual parameter functions.
     * 
     * @param fallibleAction
     *            dual parameter function that *may* fail (i.e. throw some kind
     *            of Throwable)
     * @param errorCallback
     *            function to be called with any Throwable that may be thrown as
     *            a parameter (i.e. an error handler but also an error
     *            logger/transformer, etc)
     * @return a BiConsumer with error handling (via the errorCallback)
     */
    public static <T, U> BiConsumer<? super T, ? super U> tryAndDo(
            FallibleBiConsumer<? super T, ? super U, ?> fallibleAction,
            Consumer<Throwable> errorCallback) {
        return (t, u) -> {
            try {
                fallibleAction.accept(t, u);
            } catch (Throwable ex) {
                errorCallback.accept(ex);
            }
        };
    }

    /**
     * Allows to transform dual parameter functions that throw checked
     * exceptions into functions that only throw runtime exceptions.
     * 
     * This is especially useful when trying to use methods like 'foreach'
     * (which doesn't accept functions that throw checked exceptions).
     * 
     * @param fallibleAction
     *            dual parameter function that *may* fail (i.e. throw some kind
     *            of Throwable)
     * @return a BiConsumer that will only throw runtime exceptions
     */
    public static <T, U, E extends Exception> BiConsumer<? super T, ? super U> unsafely(
            FallibleBiConsumer<? super T, ? super U, ? super E> fallibleAction) {
        return tryAndDo(fallibleAction, ex -> failWith(ex));
    }

    /**
     * A version of {@link #unsafely(FallibleBiConsumer)} that allows to add a
     * callback to be called before the exception is thrown.
     *
     * @param fallibleAction
     *            dual parameter function that *may* fail (i.e. throw some kind
     *            of Throwable)
     * @param actionBeforeException
     *            function to be called before the exception is thrown
     * @return a BiConsumer that will only throw runtime exceptions
     */
    public static <T, U, E extends Exception> BiConsumer<? super T, ? super U> unsafely(
            FallibleBiConsumer<? super T, ? super U, ? super E> fallibleAction,
            Consumer<Throwable> actionBeforeException) {
        return tryAndDo(fallibleAction, ex -> {
            actionBeforeException.accept(ex);
            failWith(ex);
        });
    }

    @SuppressWarnings("unchecked")
    public static <E extends Exception> Consumer<Throwable> onErrorDo(
            Predicate<Throwable> errorMatcher, Consumer<? super E> errorCallback) {
        return ex -> {
            if (errorMatcher.test(ex)) {
                errorCallback.accept((E) ex);
            } else {
                failWith(ex);
            }
        };
    }

    public static <E extends Exception> Consumer<Throwable> onErrorDo(
            Class<? super E> errorType, Consumer<? super E> errorCallback) {
        Predicate<Throwable> errorMatcher = ex -> errorType.isInstance(ex);
        return onErrorDo(errorMatcher, errorCallback);
    }

    public static void failWith(Throwable ex) {
        throw new RuntimeException(ex);
    }

    @FunctionalInterface
    interface FallibleBiConsumer<K, V, E extends Throwable> {
        public void accept(K t, V u) throws E;
    }
}
