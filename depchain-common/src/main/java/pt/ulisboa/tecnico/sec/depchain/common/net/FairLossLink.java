package pt.ulisboa.tecnico.sec.depchain.common.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

import pt.ulisboa.tecnico.sec.depchain.common.utils.ByteUtils;

public class FairLossLink {

    private final DatagramSocket socket;
    private final SocketAddress address;

    public FairLossLink(DatagramSocket socket, SocketAddress address) {
        this.socket = socket;
        this.address = address;
    }

    public void send(Object message) throws IOException {
        byte[] bytes = ByteUtils.serialize(message);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address);
        this.socket.send(packet);
    }
}
