package info.kgeorgiy.ja.shcherbakov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.*;

public class IterativeParallelism implements ListIP {
    private final ParallelMapper parallelMapper;

    public IterativeParallelism() {
        this(null);
    }

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private static <K, V> Map.Entry<K, V> makeMapEntry(K key, V val) {
        return new AbstractMap.SimpleImmutableEntry<>(key, val);
    }

    private static <T, R> Function<List<? extends T>, ? extends R> getHandler(Supplier<R> initResult,
                                                                              BiFunction<R, T, Map.Entry<R, Boolean>> action) {
        return (values) -> {
            R result = initResult.get();
            for (T current : values) {
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    return result;
                }
                Map.Entry<R, Boolean> actionResult = action.apply(result, current);
                result = actionResult.getKey();
                if (actionResult.getValue()) {
                    return result;
                }
            }
            return result;
        };
    }

    private <T, R> R parallelizeWork(int threads, List<? extends T> values,
                                     Supplier<R> initResult,
                                     BiFunction<R, T, Map.Entry<R, Boolean>> threadAction,
                                     BiFunction<R, R, Map.Entry<R, Boolean>> mergeResult) throws InterruptedException {
        if (threads > values.size()) {
            threads = values.size();
        }
        List<R> result;
        List<List<? extends T>> pieces = new ArrayList<>(threads);
        int offset = 0;
        for (int i = 0; i < threads; i++) {
            int actionCount = (values.size() - offset) / (threads - i);
            pieces.add(values.subList(offset, offset + actionCount));
            offset += actionCount;
        }

        if (parallelMapper != null) {
            result = parallelMapper.map(getHandler(initResult, threadAction), pieces);
        } else {
            result = new ArrayList<>(Collections.nCopies(threads, null));
            Thread[] threadsArr = new Thread[threads];
            for (int i = 0; i < threads; i++) {
                int index = i;
                threadsArr[i] = new Thread(() -> result.set(index,
                        getHandler(initResult, threadAction).apply(pieces.get(index))));
                threadsArr[i].start();
            }
            for (int i = 0; i < threads; i++) {
                threadsArr[i].join();
                if (threadsArr[i].isInterrupted()) {
                    throw new InterruptedException();
                }
            }
        }

        R answer = initResult.get();
        for (int i = 0; i < threads; i++) {
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
        return !all(threads, values, predicate.negate());
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
