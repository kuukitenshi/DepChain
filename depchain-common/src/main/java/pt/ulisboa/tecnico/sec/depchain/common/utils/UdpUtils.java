package pt.ulisboa.tecnico.sec.depchain.common.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public final class UdpUtils {

    private static final int MAX_PACKET_LEN = 65535;

    public static DatagramPacket receive(DatagramSocket socket) throws IOException {
        byte[] buf = new byte[MAX_PACKET_LEN];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        return packet;
    }

}
