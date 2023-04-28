package info.kgeorgiy.ja.shcherbakov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements AdvancedCrawler {
    private final Downloader downloader;
    private final int perHost;

    private final ExecutorService downloadService, extractService;
    private final ConcurrentMap<String, Semaphore> hostSemaphores = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        if (args == null || args.length < 1 || args.length > 4) {
            System.err.println("WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        String url = args[0];
        int depth = getOrDefault(args, 1, 2);
        int downloads = getOrDefault(args, 2, 16);
        int extractors = getOrDefault(args, 3, 16);
        int perHost = getOrDefault(args, 4, 4);

        try {
            Downloader downloader = new CachingDownloader(1);
            try (WebCrawler webCrawler = new WebCrawler(downloader, downloads, extractors, perHost)) {
                webCrawler.download(url, depth);
            }
        } catch (IOException e) {
            System.out.println("Unable to create of CachingDownloader: " + e.getMessage());
        }
    }

    private static int getOrDefault(String[] args, int i, int defaultValue) {
        if (args.length <= i) {
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

    private void concurrentDownload(String url, int depth, Set<String> success, Map<String, IOException> errors,
                                    Set<String> usedLinks, Phaser phaser, List<String> requiredHosts) {
        try {
            String host = URLUtils.getHost(url);
            if (requiredHosts != null && !requiredHosts.contains(host)) {
                return;
            }
            hostSemaphores.putIfAbsent(host, new Semaphore(perHost));
            phaser.register();
            downloadService.submit(() -> {
                Semaphore semaphore = hostSemaphores.get(host);
                try {
                    Document document;
                    try {
                        semaphore.acquire();
                        document = downloader.download(url);
                    } finally {
                        semaphore.release();
                    }
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
                } catch (InterruptedException e) {
                    System.err.println("Unexpected error occurred while extracting links: " + e.getMessage());
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
