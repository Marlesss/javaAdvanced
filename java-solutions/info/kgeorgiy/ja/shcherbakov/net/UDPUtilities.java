package info.kgeorgiy.ja.shcherbakov.net;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UDPUtilities {

    // Utilities class
    private UDPUtilities() {
    }

    public static DatagramPacket getRequestPacket(byte[] data, SocketAddress destination) {
        return new DatagramPacket(data, data.length, destination);
    }

    public static DatagramPacket getResponsePacket(DatagramSocket socket) throws SocketException {
        return new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
    }

    public static String getString(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public static void cleanupSocketReceive(DatagramSocket socket, int timeout) throws SocketException {
        int oldTimeout = socket.getSoTimeout();
        socket.setSoTimeout(timeout);
        DatagramPacket packet = getResponsePacket(socket);
        int cleaned = 0;
        while (true) {
            try {
                socket.receive(packet);
                cleaned++;
            } catch (SocketTimeoutException ignored) {
                break;
            } catch (IOException ignored) {
            }
        }
        socket.setSoTimeout(oldTimeout);
        if (cleaned > 0) {
            System.err.println("Cleaned " + cleaned);
        }
    }

    public static DatagramPacket lastReceive(DatagramSocket socket) throws IOException {
        int oldTimeout = socket.getSoTimeout();
        socket.setSoTimeout(10);
        DatagramPacket answer = getResponsePacket(socket);
        socket.receive(answer);
        DatagramPacket packet = getResponsePacket(socket);
        while (true) {
            try {
                socket.receive(packet);
                answer = packet;
            } catch (SocketTimeoutException ignored) {
                break;
            }
        }
        socket.setSoTimeout(oldTimeout);
        return answer;
    }
}
