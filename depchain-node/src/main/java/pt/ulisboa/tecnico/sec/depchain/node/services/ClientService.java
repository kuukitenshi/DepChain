package pt.ulisboa.tecnico.sec.depchain.node.services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hyperledger.besu.datatypes.Address;

import pt.ulisboa.tecnico.sec.depchain.common.logging.Logger;
import pt.ulisboa.tecnico.sec.depchain.common.logging.LoggerFactory;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedData;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedMessage;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.depchain.common.net.FairLossLink;
import pt.ulisboa.tecnico.sec.depchain.common.net.StubbornLink;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.ContractTransaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.ExternalTransaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.Transaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.TransactionResult;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.TransactionResult.Opcode;
import pt.ulisboa.tecnico.sec.depchain.common.utils.ByteUtils;
import pt.ulisboa.tecnico.sec.depchain.common.utils.UdpUtils;
import pt.ulisboa.tecnico.sec.depchain.node.container.ServiceHandle;
import pt.ulisboa.tecnico.sec.depchain.node.services.configs.ClientServiceConfig;
import pt.ulisboa.tecnico.sec.depchain.node.services.configs.GlobalServiceConfig;

public class ClientService implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientService.class);

    private final ServiceHandle<ClientService> handle;
    private final Map<SocketAddress, AuthenticatedPerfectLink> links = new HashMap<>();
    private final Map<Transaction, SocketAddress> pendingClients = new HashMap<>();
    private final Map<Address, Set<Long>> nonces = new HashMap<>();

    private ClientServiceConfig config;
    private GlobalServiceConfig gConfig;

    public ClientService(ServiceHandle<ClientService> handle) {
        this.handle = handle;
        handle.publishes(ExternalTransaction.class);
        handle.publishes(ContractTransaction.class);
        handle.listens(TransactionResult.class, this::onResult);
    }

    public void init(ClientServiceConfig config, GlobalServiceConfig gConfig) {
        this.config = config;
        this.gConfig = gConfig;
    }

    @Override
    public void run() {
        LOGGER.info("now listening to incoming client messages");
        while (!this.config.socket().isClosed()) {
            DatagramPacket packet;
            try {
                packet = UdpUtils.receive(this.config.socket());
            } catch (IOException e) {
                LOGGER.error("failed to receive socket packet!");
                break;
            }
            SocketAddress address = packet.getSocketAddress();
            Object object;
            try {
                object = ByteUtils.deserialize(packet.getData());
            } catch (ClassNotFoundException e) {
                LOGGER.warn("couldn't deserialize client message!");
                continue;
            }
            if (object instanceof AuthenticatedMessage am) {
                if (!this.links.containsKey(address)) {
                    PrivateKey prk = this.gConfig.secrets().getPrivateKey(this.gConfig.self().id());
                    KeyPair pair = new KeyPair(null, prk);
                    FairLossLink fll = new FairLossLink(this.config.socket(), address);
                    StubbornLink sl = new StubbornLink(fll);
                    AuthenticatedPerfectLink apl = new AuthenticatedPerfectLink(sl, gConfig.self().id(), "client", pair,
                            false);
                    try {
                        apl.startHandshake();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    this.links.put(address, apl);
                }
                AuthenticatedPerfectLink apl = this.links.get(address);
                try {
                    if (apl.deliver(am) && am instanceof AuthenticatedData data) {
                        onMessage(data, address);
                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onMessage(AuthenticatedData message, SocketAddress address) {
        if (message.data() instanceof Transaction transaction) {
            this.pendingClients.put(transaction, address);
            try {
                if (transaction.verifySignature()) {
                    Address addr = Address.fromHexString(transaction.getSender());
                    nonces.putIfAbsent(addr, new HashSet<>());
                    long nonce = transaction.getNonce();
                    if (nonces.get(addr).contains(nonce)) {
                        LOGGER.warn("Transaction with nonce %s already executed, ignoring it", transaction.getNonce());
                        AuthenticatedPerfectLink apl = this.links.get(address);
                        PrivateKey prk = this.gConfig.secrets().getPrivateKey(this.gConfig.self().id());
                        TransactionResult result = new TransactionResult(transaction, false, null, Opcode.REVERT, prk);
                        onResult(result);
                        return;
                    }
                    nonces.get(addr).add(nonce);
                    this.handle.publish(transaction);
                    LOGGER.info("Received %s", transaction);
                }
            } catch (SignatureException e) {
                e.printStackTrace();
            }
        }
    }

    private void onResult(TransactionResult result) {
        Transaction transaction = result.transaction();
        if (this.pendingClients.containsKey(transaction)) {
            SocketAddress address = this.pendingClients.get(transaction);
            AuthenticatedPerfectLink apl = this.links.get(address);
            try {
                LOGGER.trace("send result to client");
                apl.send(result);
                this.pendingClients.remove(transaction);
            } catch (IOException e) {
                LOGGER.error("Failed to send result to client!");
                e.printStackTrace();
            }
        }
    }

}
