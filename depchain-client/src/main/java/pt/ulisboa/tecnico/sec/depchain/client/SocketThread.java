package pt.ulisboa.tecnico.sec.depchain.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;

import pt.ulisboa.tecnico.sec.depchain.common.info.NodeInfo;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedData;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedMessage;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.depchain.common.utils.ByteUtils;
import pt.ulisboa.tecnico.sec.depchain.common.utils.UdpUtils;

public class SocketThread extends Thread {

    private final DatagramSocket socket;
    private final Map<NodeInfo, AuthenticatedPerfectLink> nodeLinks;
    private final ClientStub clientStub;

    public SocketThread(DatagramSocket socket, Map<NodeInfo, AuthenticatedPerfectLink> nodeLinks,
            ClientStub clientStub) {
        this.socket = socket;
        this.nodeLinks = nodeLinks;
        this.clientStub = clientStub;
    }

    @Override
    public void run() {
        while (!this.socket.isClosed()) {
            try {
                DatagramPacket packet = UdpUtils.receive(this.socket);
                try {
                    Object object = ByteUtils.deserialize(packet.getData());
                    if (object instanceof AuthenticatedMessage am) {
                        nodeLinks.forEach((node, link) -> {
                            if (node.clientAddress().equals(packet.getSocketAddress())) {
                                try {
                                    link.deliver(am);
                                    if (am instanceof AuthenticatedData data) {
                                        clientStub.onDeliver(data);
                                    }
                                } catch (ClassNotFoundException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("failed to deserialize packet!");
                }
            } catch (IOException e) {
                break;
            }
        }
    }

}
