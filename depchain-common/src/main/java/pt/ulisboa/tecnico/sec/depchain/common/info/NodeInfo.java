package pt.ulisboa.tecnico.sec.depchain.common.info;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public record NodeInfo(String id, String hostname, int nodePort, int clientPort) {

    public SocketAddress nodeAddress() {
        return new InetSocketAddress(this.hostname, this.nodePort);
    }

    public SocketAddress clientAddress() {
        return new InetSocketAddress(this.hostname, this.clientPort);
    }
}
