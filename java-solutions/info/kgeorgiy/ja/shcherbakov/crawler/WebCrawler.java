package info.kgeorgiy.ja.shcherbakov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements AdvancedCrawler {
    private final Downloader downloader;
    private final int perHost;

    private final ExecutorService downloadService, extractService;
    private final ConcurrentMap<String, Loader> hostSemaphores = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        if (args == null || args.length < 1 || args.length > 4) {
            System.err.println("WebCrawler url [downloaders [extractors [perHost]]]");
            return;
        }

        int downloaders = getOrDefault(args, 1, 16);
        int extractors = getOrDefault(args, 2, 16);
        int perHost = getOrDefault(args, 3, 4);
        int depth = getOrDefault(args, 4, 2);
        String url = args[0];

        try {
            Downloader downloader = new CachingDownloader(1);
            try (WebCrawler webCrawler = new WebCrawler(downloader, downloaders, extractors, perHost)) {
                webCrawler.download(url, depth);
            }
        } catch (IOException e) {
            System.out.println("Unable to create of CachingDownloader: " + e.getMessage());
        }
    }

    private static int getOrDefault(String[] args, int i, int defaultValue) {
        if (args.length >= i) {
            return defaultValue;
        }
        return Integer.parseInt(args[i]);
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        downloadService = Executors.newFixedThreadPool(downloaders);
        extractService = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(String url, int depth) {
        return download(url, depth, null);
    }

    @Override
    public Result download(String url, int depth, List<String> hosts) {
        Set<String> success = ConcurrentHashMap.newKeySet();
        ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();
        Set<String> usedLinks = ConcurrentHashMap.newKeySet();
        usedLinks.add(url);
        Phaser phaser = new Phaser(1);
        concurrentDownload(url, depth, success, errors, usedLinks, phaser, hosts);
        phaser.arriveAndAwaitAdvance();
        return new Result(new ArrayList<>(success), errors);
    }


    private class Loader {
        final Deque<Runnable> tasks = new ArrayDeque<>();
        int running = 0;

        private synchronized void addTask(Runnable task) {
            if (running >= perHost) {
                tasks.push(task);
            } else {
                runTask(task);
            }
        }

        private synchronized void runTask(Runnable task) {
            running++;
            downloadService.submit(() -> {
                task.run();
                taskFinished();
            });
        }

        private synchronized void taskFinished() {
            running--;
            if (!tasks.isEmpty()) {
                runTask(tasks.pop());
            }
        }
    }

    private void concurrentDownload(String url, int depth, Set<String> success, Map<String, IOException> errors,
                                    Set<String> usedLinks, Phaser phaser, List<String> requiredHosts) {
        try {
            String host = URLUtils.getHost(url);
            if (requiredHosts != null && !requiredHosts.contains(host)) {
                return;
            }
            phaser.register();
            hostSemaphores.computeIfAbsent(host, h -> new Loader()).addTask(() ->
            {
                try {
                    Document document = downloader.download(url);
                    success.add(url);
                    if (depth > 1) {
                        phaser.register();

                        extractService.submit(() -> {
                            try {
                                for (String inner : document.extractLinks()) {
                                    if (usedLinks.add(inner)) {
                                        concurrentDownload(inner, depth - 1, success, errors, usedLinks, phaser, requiredHosts);
                                    }
                                }
                            } catch (IOException e) {
                                System.err.println("Unexpected error occurred while extracting links: " + e.getMessage());
                            } finally {
                                phaser.arrive();
                            }
                        });
                    }
                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arrive();
                }
            });
        } catch (IOException e) {
            errors.put(url, e);
        }
    }


    @Override
    public void close() {
        downloadService.shutdownNow();
        extractService.shutdownNow();
    }
}
