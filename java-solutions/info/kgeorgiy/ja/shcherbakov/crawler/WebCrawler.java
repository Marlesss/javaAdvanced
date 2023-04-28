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

    public static void main(String[] args) throws IOException {
        Downloader downloader11 = new ReplayDownloader("http://www.kgeorgiy.info", 1, 1);
        WebCrawler webCrawler = new WebCrawler(downloader11, 4, 4, 4);
        Result result = webCrawler.download("http://www.kgeorgiy.info", 5);
        System.out.println(result.getDownloaded().size());
        System.out.println(result.getErrors().size());
        webCrawler.close();
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
                    semaphore.acquire();
                    Document document = downloader.download(url);
                    semaphore.release();
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
                    semaphore.release();
                } catch (InterruptedException ignored) {
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
