package info.kgeorgiy.ja.shcherbakov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.*;

public class IterativeParallelism implements ListIP {
    private static <K, V> Map.Entry<K, V> makeMapEntry(K key, V val) {
        return new AbstractMap.SimpleImmutableEntry<>(key, val);
    }

    private static <T, R> Thread makeThreadForEach(List<? extends T> values, int offset, int actionCount, Supplier<R> initResult,
                                                   BiFunction<R, T, Map.Entry<R, Boolean>> action, Consumer<R> handleResult) {
        return new Thread(() -> {
            R result = initResult.get();
            for (int i = 0; i < actionCount && i + offset < values.size(); i++) {
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    return;
                }
                T current = values.get(i + offset);
                Map.Entry<R, Boolean> actionResult = action.apply(result, current);
                result = actionResult.getKey();
                if (actionResult.getValue()) {
                    handleResult.accept(result);
                    return;
                }
            }
            handleResult.accept(result);
        });
    }

    private static <T, R> R parallelizeWork(int threads, List<? extends T> values,
                                            Supplier<R> initResult,
                                            BiFunction<R, T, Map.Entry<R, Boolean>> threadAction,
                                            BiFunction<R, R, Map.Entry<R, Boolean>> mergeResult) throws InterruptedException {
        if (threads > values.size()) {
            threads = values.size();
        }
        Thread[] threadsArr = new Thread[threads];
        List<R> result = new ArrayList<>(Collections.nCopies(threads, null));
        int offset = 0;
        for (int i = 0; i < threads; i++) {
            int actionCount = (values.size() - offset) / (threads - i);
            int index = i;
            threadsArr[i] = makeThreadForEach(values, offset, actionCount, initResult, threadAction,
                    (res) -> result.set(index, res));
            threadsArr[i].start();
            offset += actionCount;
        }
        R answer = initResult.get();
        for (int i = 0; i < threads; i++) {
            threadsArr[i].join();
            if (threadsArr[i].isInterrupted()) {
                throw new InterruptedException();
            }
            Map.Entry<R, Boolean> merge = mergeResult.apply(answer, result.get(i));
            answer = merge.getKey();
            if (merge.getValue()) {
                return answer;
            }
        }
        return answer;
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, comparator.reversed());
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException();
        }
        BiFunction<T, T, Map.Entry<T, Boolean>> min =
                (res, val) -> {
                    if (res == null) {
                        return makeMapEntry(val, false);
                    } else {
                        return makeMapEntry(comparator.compare(res, val) > 0 ? val : res, false);
                    }
                };
        return parallelizeWork(threads, values, () -> null, min, min);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        BiFunction<Boolean, Boolean, Map.Entry<Boolean, Boolean>> allMerge = (res, val) -> {
            if (!val) {
                return makeMapEntry(false, true);
            }
            return makeMapEntry(true, false);
        };
        return parallelizeWork(threads, values, () -> true,
                (res, val) -> allMerge.apply(res, predicate.test(val)),
                allMerge);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        BiFunction<Boolean, Boolean, Map.Entry<Boolean, Boolean>> anyMerge = (res, val) -> {
            if (val) {
                return makeMapEntry(true, true);
            }
            return makeMapEntry(false, false);
        };
        return parallelizeWork(threads, values, () -> false,
                (res, val) -> anyMerge.apply(res, predicate.test(val)),
                anyMerge);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelizeWork(threads, values,
                () -> 0,
                (res, val) -> {
                    if (predicate.test(val)) {
                        res++;
                    }
                    return makeMapEntry(res, false);
                },
                (answer, threadResult) -> makeMapEntry(answer + threadResult, false)
        );
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        BiFunction<StringBuilder, Object, Map.Entry<StringBuilder, Boolean>> concat = (res, val) -> makeMapEntry(res.append(val.toString()), false);
        return parallelizeWork(threads, values, StringBuilder::new,
                concat,
                concat::apply).toString();
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelizeWork(threads, values, ArrayList::new,
                (res, val) -> {
                    if (predicate.test(val)) {
                        res.add(val);
                    }
                    return makeMapEntry(res, false);
                },
                (res, threadRes) -> {
                    res.addAll(threadRes);
                    return makeMapEntry(res, false);
                });
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelizeWork(threads, values, ArrayList::new,
                (res, val) -> {
                    res.add(f.apply(val));
                    return makeMapEntry(res, false);
                },
                (res, threadRes) -> {
                    res.addAll(threadRes);
                    return makeMapEntry(res, false);
                });
    }


}
