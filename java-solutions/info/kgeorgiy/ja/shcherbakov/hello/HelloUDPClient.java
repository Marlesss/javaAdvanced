package info.kgeorgiy.ja.shcherbakov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class HelloUDPClient implements HelloClient {

    public static void main(String[] args) {
        if (args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("HelloUDPClient hostAddress hostPort requestPrefix threadsNumber requestsNumber");
            return;
        }
        try {
            String hostName = args[0];
            int hostPort = Integer.parseInt(args[1]);
            String requestPrefix = args[2];
            int threadsNumber = Integer.parseInt(args[3]);
            int requestsNumber = Integer.parseInt(args[4]);
            new HelloUDPClient().run(hostName, hostPort, requestPrefix, threadsNumber, requestsNumber);
        } catch (NumberFormatException e) {
            System.err.println("Host port, threads number and requests number must be integer");
            System.err.println("HelloUDPClient hostAddress hostPort requestPrefix threadsNumber requestsNumber");
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        SocketAddress serverSocketAddress = new InetSocketAddress(host, port);
        ExecutorService senders = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 1; i <= threads; i++) {
            futures.add(senders.submit(messagesSender(serverSocketAddress, prefix, i, requests)));
        }

        try {
            awaitSenders(futures);
        } catch (InterruptedException e) {
            System.err.println("Run thread was interrupted");
            System.err.println("Shutdown all senders...");
            System.err.println(e.getMessage());
        } finally {
            senders.shutdownNow();
        }
    }

    private void awaitSenders(List<Future<?>> futures) throws InterruptedException {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                System.err.println("Error while sending requests: " + e.getMessage());
            }
        }
    }

    private String getMessage(String prefix, int threadNumber, int requestNumber) {
        return String.format("%s%d_%d", prefix, threadNumber, requestNumber);
    }

    private void sendMessage(DatagramSocket socket, DatagramPacket requestPacket, Predicate<DatagramPacket> checkResponse) {
        while (true) {
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt();
                break;
            }
            try {
                socket.send(requestPacket);
                DatagramPacket responsePacket = UDPUtilities.lastReceive(socket);
                if (checkResponse.test(responsePacket)) {
                    System.out.println(UDPUtilities.getString(responsePacket));
                    break;
                }
            } catch (SocketTimeoutException ignored) {
            } catch (SocketException e) {
                System.err.println("Socket exception: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Send/receive exception: " + e.getMessage());
            }
        }
    }

    private Runnable messagesSender(SocketAddress serverSocketAddress, String prefix, int threadNumber, int count) {
        return () -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(RESPONSE_TIMEOUT);
                for (int i = 1; i <= count; i++) {
                    String message = getMessage(prefix, threadNumber, i);
                    System.out.println(message);
                    byte[] data = message.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket requestPacket = UDPUtilities.getRequestPacket(data, serverSocketAddress);
                    sendMessage(socket, requestPacket, messageCheck(prefix, threadNumber, i));
                }
            } catch (SocketException e) {
                System.err.println("Socket exception while initialize: " + e.getMessage());
            }
        };
    }

    private static Predicate<DatagramPacket> messageCheck(String prefix, int thread, int request) {
        return packet -> {
            String response = UDPUtilities.getString(packet);
            String[] parts = response.split(", ");
            if (parts.length != 2) {
                return false;
            }
            String requestMirror = parts[1];
            if (!requestMirror.startsWith(prefix)) {
                return false;
            }
            String truncate = requestMirror.substring(prefix.length());
            try {
                if (truncate.isEmpty()) {
                    return false;
                }
                for (int i = 1; i < truncate.length() - 1; i++) {
                    if (truncate.charAt(i) == '_') {
                        int threadResp = Integer.parseInt(truncate.substring(0, i));
                        int requestResp = Integer.parseInt(truncate.substring(i + 1));
                        return threadResp == thread && requestResp == request;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
            return false;
        };
    }

    private static Predicate<DatagramPacket> idCheck(String prefix, int thread, int request) {
        return packet -> {
            String response = UDPUtilities.getString(packet);
            try {
                List<Integer> integers = new ArrayList<>();
                int i = response.length() - 1;
                while (i >= 0) {
                    if (Character.isDigit(response.charAt(i))) {
                        int j = i;
                        while (j >= 0 && Character.isDigit(response.charAt(j))) {
                            j--;
                        }
                        integers.add(Integer.parseInt(response.substring(j + 1, i + 1)));
                        i = j;
                    } else {
                        i--;
                    }
                }
                if (integers.size() != 2) {
                    return false;
                }
                int threadResp = integers.get(1);
                int requestResp = integers.get(0);
                return threadResp == thread && requestResp == request;
            } catch (NumberFormatException ignored) {
            }
            return false;
        };
    }


    private static final int RESPONSE_TIMEOUT = 100;
}
