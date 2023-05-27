package info.kgeorgiy.ja.shcherbakov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private final Queue<Runnable> jobsQueue = new ArrayDeque<>();
    private final List<Thread> subThreads;

    public ParallelMapperImpl(int threads) {
        subThreads = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            subThreads.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Runnable job;
                        synchronized (jobsQueue) {
                            while (jobsQueue.isEmpty()) {
                                jobsQueue.wait();
                            }
                            if (Thread.interrupted()) {
                                break;
                            }
                            job = jobsQueue.poll();
                        }
                        if (job != null) {
                            job.run();
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            }));
            subThreads.get(i).start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        SynchronizedResult<R> result = new SynchronizedResult<>(args.size());
        synchronized (jobsQueue) {
            for (int i = 0; i < args.size(); i++) {
                int finalI = i;
                    jobsQueue.add(() -> {
                        R res = f.apply(args.get(finalI));
                        if (Thread.interrupted()) {
                            result.interrupted();
                            Thread.currentThread().interrupt();
                            return;
                        }
                        result.set(finalI, res);
                    });
            }
            jobsQueue.notifyAll();
        }
        return result.collect();
    }

    @Override
    public void close() {
        for (Thread subThread : subThreads) {
            subThread.interrupt();
            try {
                subThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static class SynchronizedResult<T> extends ArrayList<T> {
        private int setCounter;
        private boolean interrupted = false;

        SynchronizedResult(int size) {
            super(Collections.nCopies(size, null));
            setCounter = size;
        }

        public synchronized T set(int index, T element) {
            setCounter--;
            notify();
            return super.set(index, element);
        }

        public synchronized List<T> collect() throws InterruptedException {
            while (setCounter != 0) {
                wait();
                if (interrupted) {
                    throw new InterruptedException();
                }
            }
            return this;
        }

        public void interrupted() {
            interrupted = true;
        }
    }
}
