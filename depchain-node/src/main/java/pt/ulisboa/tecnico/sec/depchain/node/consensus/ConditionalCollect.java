package pt.ulisboa.tecnico.sec.depchain.node.consensus;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import pt.ulisboa.tecnico.sec.depchain.common.logging.Logger;
import pt.ulisboa.tecnico.sec.depchain.common.logging.LoggerFactory;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedData;
import pt.ulisboa.tecnico.sec.depchain.common.secrets.SecretsProvider;
import pt.ulisboa.tecnico.sec.depchain.node.BlockchainProcess;

public class ConditionalCollect {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionalCollect.class);

    private final List<CcListener> listeners = new ArrayList<>();
    private final Map<String, CcSendMessage> messages = new HashMap<>();
    private final long instanceId;
    private final Collection<BlockchainProcess> processes;
    private final int threshold;
    private final BlockchainProcess self;
    private final BlockchainProcess leader;
    private final SecretsProvider secrets;
    private final Predicate<Collection<CcSendMessage>> predicate;

    private boolean collected = false;

    public ConditionalCollect(long instanceId, Collection<BlockchainProcess> processes, BlockchainProcess self,
            BlockchainProcess leader, int numFaults, SecretsProvider secrets) {
        this(instanceId, processes, self, leader, numFaults, secrets, x -> true);
    }

    public ConditionalCollect(long instanceId, Collection<BlockchainProcess> processes, BlockchainProcess self,
            BlockchainProcess leader, int numFaults, SecretsProvider secrets,
            Predicate<Collection<CcSendMessage>> predicate) {
        this.instanceId = instanceId;
        this.processes = processes;
        this.self = self;
        this.leader = leader;
        this.threshold = processes.size() - numFaults;
        this.secrets = secrets;
        this.predicate = predicate;
    }

    public void input(Object data) throws IOException {
        PrivateKey prk = this.secrets.getPrivateKey(this.self.id());
        CcSendMessage message = new CcSendMessage(this.instanceId, this.self.id(), data, prk);
        this.leader.link().send(message);
        LOGGER.trace("INPUT %s for instance %s", data, this.instanceId);
    }

    public void deliver(AuthenticatedData aplMessage) {
        if (aplMessage.data() instanceof CcSendMessage ccMessage && ccMessage.instanceId() == this.instanceId) {
            onSend(aplMessage, ccMessage);
        } else if (aplMessage.data() instanceof CcCollectMessage ccMessage
                && ccMessage.instanceId() == this.instanceId) {
            onCollected(aplMessage, ccMessage);
        }
    }

    public void addListener(CcListener listener) {
        this.listeners.add(listener);
    }

    public void collected(Collection<CcSendMessage> messages) {
        this.listeners.forEach(l -> l.ccCollected(messages));
    }

    private void onSend(AuthenticatedData aplMessage, CcSendMessage ccMessage) {
        if (!this.leader.equals(self)) {
            return;
        }
        PublicKey puk = this.secrets.getPublicKey(ccMessage.sender());
        if (!ccMessage.verifySignature(puk)) {
            LOGGER.warn("received SEND with invalid signature!");
            return;
        }
        this.messages.put(ccMessage.sender(), ccMessage);
        LOGGER.trace("received SEND from process %s, %s / %s", ccMessage.sender(), messages.size(), threshold);
        if (this.messages.containsKey(this.self.id()) && this.messages.size() >= this.threshold
                && this.predicate.test(this.messages.values())) {
            CcCollectMessage collected = new CcCollectMessage(this.instanceId, this.messages);
            for (BlockchainProcess p : this.processes) {
                try {
                    p.link().send(collected);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onCollected(AuthenticatedData aplMessage, CcCollectMessage ccMessage) {
        if (this.collected || !aplMessage.sender().equals(this.leader.id())) {
            return;
        }
        LOGGER.trace("received COLLECTED from leader");
        Map<String, CcSendMessage> messages = ccMessage.messages();
        if (messages.size() >= this.threshold && this.predicate.test(messages.values())
                && verifySignatures(messages.values())) {
            this.collected = true;
            collected(messages.values());
        }
    }

    private boolean verifySignatures(Collection<CcSendMessage> messages) {
        return messages.stream().allMatch(m -> {
            PublicKey puk = secrets.getPublicKey(m.sender());
            return m.verifySignature(puk);
        });
    }

}
