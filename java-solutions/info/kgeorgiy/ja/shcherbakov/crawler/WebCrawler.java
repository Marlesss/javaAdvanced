package info.kgeorgiy.ja.shcherbakov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class WebCrawler implements AdvancedCrawler {

    public static void main(String[] args) {
        if (args == null || args.length < 1 || args.length > 4) {
            System.err.println("WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        String url = args[0];
        int depth = getOrDefault(args, 1, 2);
        int downloads = getOrDefault(args, 2, 8);
        int extractors = getOrDefault(args, 3, 7);
        int perHost = getOrDefault(args, 4, 4);

        try {
            Downloader downloader = new CachingDownloader(1);
            try (WebCrawler webCrawler = new WebCrawler(downloader, downloads, extractors, perHost)) {
                webCrawler.download(url, depth);
            }
        } catch (IOException e) {
            System.err.println("Error occurred during init of CachingDownloader: " + e.getMessage());
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
        this.hostDownloadsLimit = perHost;
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
        ConcurrentMap<String, Boolean> usedUrls = new ConcurrentHashMap<>();
        Set<String> urlQueue = Set.of(url);
        breadthDownload(urlQueue, depth, hosts, usedUrls, success, errors);
        return new Result(new ArrayList<>(success), errors);
    }

    private void breadthDownload(final Set<String> urlQueue,
                                 final int depth,
                                 final List<String> hosts,
                                 final ConcurrentMap<String, Boolean> usedUrls,
                                 final Set<String> success,
                                 final ConcurrentMap<String, IOException> errors) {
        Set<String> extracted = ConcurrentHashMap.newKeySet();
        Phaser phaser = new Phaser(1);
        for (String url : urlQueue) {
            usedUrls.put(url, true);
            phaser.register();
            downloadService.execute(() -> {
                Document document = downloadDocument(url, hosts, success, errors);
                if (document != null && depth > 1) {
                    phaser.register();
                    extractService.submit(() -> {
                        extractLinks(extracted, document);
                        phaser.arrive();
                    });
                }
                phaser.arrive();
            });
        }
        phaser.arriveAndAwaitAdvance();
        if (depth > 1) {
            Set<String> urlQueueNext = extracted.stream().filter(url -> !usedUrls.containsKey(url)).collect(Collectors.toSet());
            breadthDownload(urlQueueNext, depth - 1, hosts, usedUrls, success, errors);
        }
    }

    private Document downloadDocument(final String url,
                                      final List<String> hosts,
                                      final Set<String> success,
                                      final Map<String, IOException> errors) {
        try {
            String host = URLUtils.getHost(url);
            if (hosts != null && !hosts.contains(host)) {
                return null;
            }
            Document document;
            Semaphore hostSemaphore = hostSemaphores.computeIfAbsent(host, h -> new Semaphore(hostDownloadsLimit));
            try {
                hostSemaphore.acquire();
                document = downloader.download(url);
            } catch (InterruptedException e) {
                System.err.println("Unexpected error occurred while downloading document: " + e.getMessage());
                return null;
            } finally {
                hostSemaphore.release();
            }
            success.add(url);
            return document;
        } catch (IOException e) {
            errors.put(url, e);
            return null;
        }
    }

    private void extractLinks(final Set<String> extracted,
                              final Document document) {
        try {
            extracted.addAll(document.extractLinks());
        } catch (IOException e) {
            System.err.println("Unexpected error occurred while extracting links: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        downloadService.shutdownNow();
        extractService.shutdownNow();
    }

    private final Downloader downloader;
    private final int hostDownloadsLimit;

    private final ExecutorService downloadService, extractService;

    private final ConcurrentMap<String, Semaphore> hostSemaphores = new ConcurrentHashMap<>();
}
