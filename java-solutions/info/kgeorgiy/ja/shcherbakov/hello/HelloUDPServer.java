package info.kgeorgiy.ja.shcherbakov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;


public class HelloUDPServer implements HelloServer {

    public static void main(String[] args) {
        if (args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("HelloUDPServer port workThreads");
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);
            int workThreads = Integer.parseInt(args[1]);

            new HelloUDPServer().start(port, workThreads);
        } catch (NumberFormatException e) {
            System.err.println("port and workThreads must be integer");
            System.err.println("HelloUDPServer port workThreads");
            System.err.println(e.getMessage());
        }
    }

    private void requestHandler(Function<DatagramPacket, DatagramPacket> requestHandler) {
        while (!socket.isClosed() && !Thread.interrupted()) {
            try {
                DatagramPacket request = UDPUtilities.getResponsePacket(socket);
                socket.receive(request);
                DatagramPacket response = requestHandler.apply(request);
                socket.send(response);
            } catch (SocketException e) {
                System.err.println("Socket error: " + e.getMessage());
            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                System.err.println("IO exception: " + e.getMessage());
            }
        }
        Thread.currentThread().interrupt();
    }

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(RECEIVE_TIMEOUT);
            service = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                service.submit(() -> requestHandler(this::helloResponse));
            }
        } catch (SocketException e) {
            System.err.println("Failed to start server: unable to create socket: " + e.getMessage());
        }
    }

    private DatagramPacket helloResponse(DatagramPacket request) {
        String requestMessage = UDPUtilities.getString(request);
        String responseMessage = "Hello, " + requestMessage;
        byte[] responseData = responseMessage.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(responseData, responseData.length, request.getSocketAddress());
    }


    @Override
    public void close() {
        if (socket != null) {
            socket.close();
        }
        if (service != null) {
            service.shutdownNow();
        }
    }

    private static final int RECEIVE_TIMEOUT = 10;
    private ExecutorService service;
    private DatagramSocket socket;
}
