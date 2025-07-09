package pt.ulisboa.tecnico.sec.depchain.node.services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;

import pt.ulisboa.tecnico.sec.depchain.common.logging.Logger;
import pt.ulisboa.tecnico.sec.depchain.common.logging.LoggerFactory;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedData;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedMessage;
import pt.ulisboa.tecnico.sec.depchain.common.utils.ByteUtils;
import pt.ulisboa.tecnico.sec.depchain.node.BlockchainProcess;
import pt.ulisboa.tecnico.sec.depchain.node.command_line.CommandLineArguments.ExecutionMode;
import pt.ulisboa.tecnico.sec.depchain.node.container.ServiceHandle;
import pt.ulisboa.tecnico.sec.depchain.node.services.configs.GlobalServiceConfig;
import pt.ulisboa.tecnico.sec.depchain.node.services.configs.NodeServiceConfig;

public class NodeService implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeService.class);

    private final ServiceHandle<NodeService> handle;

    private NodeServiceConfig config;
    private GlobalServiceConfig globalConfig;

    public NodeService(ServiceHandle<NodeService> handle) {
        this.handle = handle;
        handle.publishes(AuthenticatedData.class);
    }

    public void init(NodeServiceConfig config, GlobalServiceConfig gConfig) {
        this.config = config;
        this.globalConfig = gConfig;
    }

    @Override
    public void run() {
        LOGGER.info("now listening to incoming node messages");
        if (this.globalConfig.execMode() == ExecutionMode.NO_RESPONSE) {
            LOGGER.warn(
                    "This node is configured to not respond to messages, it will accept messages (sending ACKs) but do not send any further replies to make progress in the consensus algorithm!");
        }
        startHandshakes();
        while (!this.config.socket().isClosed()) {
            byte[] buf = new byte[65535];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                this.config.socket().receive(packet);
            } catch (IOException e) {
                LOGGER.error("couldn't receive packet from socket!");
                break;
            }
            Object message;
            try {
                message = ByteUtils.deserialize(packet.getData());
            } catch (ClassNotFoundException e) {
                LOGGER.warn("received invalid message!");
                continue;
            }
            if (message instanceof AuthenticatedMessage apl) {
                SocketAddress address = packet.getSocketAddress();
                if (this.globalConfig.processes().containsKey(address)) {
                    BlockchainProcess sender = this.globalConfig.processes().get(address);
                    try {
                        if (sender.link().deliver(apl)
                                && this.globalConfig.execMode() != ExecutionMode.NO_RESPONSE) {
                            this.handle.publish(apl);
                        }
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LOGGER.warn("received message from unknown process from address %s", address);
                }
            } else {
                LOGGER.warn("received invalid message!");
            }
        }
    }

    private void startHandshakes() {
        this.globalConfig.processes().values().forEach(p -> {
            try {
                p.link().startHandshake();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
