package pt.ulisboa.tecnico.sec.depchain.common.net;

import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import pt.ulisboa.tecnico.sec.depchain.common.logging.Logger;
import pt.ulisboa.tecnico.sec.depchain.common.logging.LoggerFactory;
import pt.ulisboa.tecnico.sec.depchain.common.utils.CancellationToken;
import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;
import pt.ulisboa.tecnico.sec.depchain.common.utils.StorageBuffer;

public class AuthenticatedPerfectLink {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedPerfectLink.class);

    private final Map<Long, CancellationToken> ackTokens = new HashMap<>();
    private final AtomicLong sequenceNumber = new AtomicLong(0);
    private final StorageBuffer<Object> messageBuffer = new StorageBuffer<>();
    private final Set<Long> delivered = new HashSet<>();
    private final KeyPair diffieKeyPair = CryptoUtils.genDiffieKeyPair();
    private final StubbornLink stubbornLink;
    private final String self;
    private final String other;
    private final KeyPair keyPair;
    private final boolean checkHandshake;

    private CancellationToken handshakeToken = null;
    private Key sessionKey = null;

    public AuthenticatedPerfectLink(StubbornLink stubbornLink, String self, String other, KeyPair keyPair) {
        this(stubbornLink, self, other, keyPair, true);
    }

    public AuthenticatedPerfectLink(StubbornLink stubbornLink, String self, String other, KeyPair keyPair,
            boolean checkHandshake) {
        this.stubbornLink = stubbornLink;
        this.self = self;
        this.other = other;
        this.keyPair = keyPair;
        this.checkHandshake = checkHandshake;
    }

    public void send(Object message) throws IOException {
        if (this.sessionKey == null) {
            this.messageBuffer.store(message);
            return;
        }
        long seq = this.sequenceNumber.getAndIncrement();
        AuthenticatedData dataMessage = new AuthenticatedData(seq, this.self, message, this.sessionKey);
        CancellationToken token = this.stubbornLink.send(dataMessage);
        this.ackTokens.put(seq, token);
    }

    public boolean deliver(AuthenticatedMessage message) throws IOException, ClassNotFoundException {
        if (message instanceof AuthenticatedHandshake handshake) {
            onHandshake(handshake);
            return false;
        }
        if (message instanceof AuthenticatedHandshakeAck ack) {
            onHandshakeAck(ack);
            return false;
        }
        if (this.sessionKey == null) {
            // ignore other messages while handshake in process,
            // its fine, they will get resent later
            LOGGER.trace("received message while handshake in progress, ignoring it");
            return false;
        }
        if (message instanceof AuthenticatedAck ack) {
            onAck(ack);
            return false;
        }
        if (message instanceof AuthenticatedData data) {
            return onData(data);
        }
        return false;
    }

    public void startHandshake() throws IOException {
        PrivateKey prk = this.keyPair.getPrivate();
        PublicKey diffiePub = this.diffieKeyPair.getPublic();
        AuthenticatedHandshake handshake = new AuthenticatedHandshake(this.self, diffiePub, prk);
        LOGGER.trace("starting handshake with %s", this.other);
        this.handshakeToken = this.stubbornLink.send(handshake);
    }

    private void onHandshake(AuthenticatedHandshake message) throws ClassNotFoundException, IOException {
        PublicKey puk = this.keyPair.getPublic();
        if (this.checkHandshake && !message.verifySignature(puk)) {
            LOGGER.warn("received handshake with invalid signature!");
            return;
        }
        if (this.sessionKey == null) {
            this.sessionKey = CryptoUtils.genSharedSecret(this.diffieKeyPair.getPrivate(), message.diffiePub());
            this.messageBuffer.dump(stored -> {
                try {
                    send(stored);
                } catch (IOException e) {
                    e.printStackTrace();
                    LOGGER.error("failed to send stored message!");
                }
            });
        }
        sendHandshakeAck();
    }

    private void onHandshakeAck(AuthenticatedHandshakeAck message) {
        if (this.sessionKey != null && message.verifyMac(this.sessionKey)) {
            this.handshakeToken.setCancelled(true);
            LOGGER.trace("completed handshake with %s", this.other);
        }
    }

    private void onAck(AuthenticatedAck message) {
        if (message.verifyMac(this.sessionKey)) {
            if (ackTokens.containsKey(message.sequenceNumber())) {
                CancellationToken token = ackTokens.get(message.sequenceNumber());
                token.setCancelled(true);
            }
        }
    }

    private boolean onData(AuthenticatedData message) throws IOException {
        if (message.verifyMac(this.sessionKey)) {
            if (message.sender().equals(this.other)) {
                sendAck(message.sequenceNumber());
                if (!this.delivered.contains(message.sequenceNumber())) {
                    return true;
                }
            } else {
                LOGGER.warn("received message with wrong sender id");
            }
        } else {
            LOGGER.warn("received message with invalid MAC");
        }
        return false;
    }

    private void sendAck(long seq) throws IOException {
        AuthenticatedAck ack = new AuthenticatedAck(seq, this.sessionKey);
        this.stubbornLink.sendOnce(ack);
    }

    private void sendHandshakeAck() throws IOException {
        AuthenticatedHandshakeAck ack = new AuthenticatedHandshakeAck(this.self, this.sessionKey);
        this.stubbornLink.sendOnce(ack);
    }
}
