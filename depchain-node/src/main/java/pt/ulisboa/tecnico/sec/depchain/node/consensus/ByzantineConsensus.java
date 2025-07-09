package pt.ulisboa.tecnico.sec.depchain.node.consensus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;

import pt.ulisboa.tecnico.sec.depchain.common.logging.Logger;
import pt.ulisboa.tecnico.sec.depchain.common.logging.LoggerFactory;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedData;
import pt.ulisboa.tecnico.sec.depchain.common.secrets.SecretsProvider;
import pt.ulisboa.tecnico.sec.depchain.node.BlockchainProcess;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.Block;
import pt.ulisboa.tecnico.sec.depchain.node.command_line.CommandLineArguments.ExecutionMode;
import pt.ulisboa.tecnico.sec.depchain.node.consensus.BcMessage.BcMessageType;

public class ByzantineConsensus implements CcListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ByzantineConsensus.class);
    private static final long TIMEOUT = 90000L;

    private final List<BcListener> listeners = new ArrayList<>();
    private final Map<String, Object> written = new HashMap<>();
    private final Map<String, Object> accepted = new HashMap<>();
    private final Timer timeout = new Timer();
    private final Collection<BlockchainProcess> processes;
    private final ConditionalCollect cc;
    private final BlockchainProcess self;
    private final EpochState state;
    private final int threshold;
    private final long ets;
    private final long instanceId;
    private final BlockchainProcess leader;
    private final ExecutionMode execMode;
    private final SecretsProvider secrets;
    private Object val = null;
    private boolean halted = false;
    private int numFaults;

    public ByzantineConsensus(long instanceId, Collection<BlockchainProcess> processes, BlockchainProcess self,
            BlockchainProcess leader, int numFaults, EpochState state, SecretsProvider secrets,
            ExecutionMode execMode) {
        this(instanceId, processes, self, leader, numFaults, state, secrets, execMode, x -> true);
    }

    public ByzantineConsensus(long instanceId, Collection<BlockchainProcess> processes, BlockchainProcess self,
            BlockchainProcess leader, int numFaults, EpochState state, SecretsProvider secrets,
            ExecutionMode execMode, Predicate<Collection<CcSendMessage>> predicate) {
        this.instanceId = instanceId;
        this.processes = processes;
        this.self = self;
        this.cc = new ConditionalCollect(instanceId, processes, self, leader, numFaults, secrets, predicate);
        this.cc.addListener(this);
        this.state = state;
        this.secrets = secrets;
        this.numFaults = numFaults;
        this.threshold = (int) Math.ceil((processes.size() + numFaults) / 2);
        this.ets = 0;
        this.leader = leader;
        this.execMode = execMode;
        timeout.schedule(new TimerTask() {
            @Override
            public void run() {
                complain();
                abort();
            }
        }, TIMEOUT);
    }

    public void propose(Object value) throws IOException {
        if (this.halted) {
            return;
        }
        if (val == null) {
            val = value;
            if (this.execMode == ExecutionMode.DIFFERENT_CLIENT) {
                val = genArbitraryValue();
            }
        }
        LOGGER.trace("Proposing %s", val);
        for (BlockchainProcess p : this.processes) {
            BcMessage bcMessage = new BcMessage(this.instanceId, BcMessageType.READ, null);
            p.link().send(bcMessage);
        }
    }

    public void decide(Object value) {
        if (this.halted) {
            return;
        }
        this.halted = true;
        this.timeout.cancel();
        listeners.forEach(l -> l.bcDecided(value));
    }

    public void abort() {
        if (halted) {
            return;
        }
        this.halted = true;
        this.timeout.cancel();
        listeners.forEach(l -> l.bcAborted(this.state));
    }

    public void deliver(AuthenticatedData message) {
        if (this.halted) {
            return;
        }
        if (message.data() instanceof BcMessage bcMessage) {
            if (bcMessage.instanceId() != this.instanceId) {
                return;
            }
            switch (bcMessage.type()) {
                case BcMessageType.READ:
                    onRead(message, bcMessage);
                    break;
                case BcMessageType.WRITE:
                    onWrite(message, bcMessage);
                    break;
                case BcMessageType.ACCEPT:
                    onAccept(message, bcMessage);
                    break;
            }
        }
    }

    private void onRead(AuthenticatedData aplMessage, BcMessage bcMessage) {
        LOGGER.trace("Received READ from leader");
        try {
            this.cc.input(this.state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onWrite(AuthenticatedData aplMessage, BcMessage bcMessage) {
        Object value = bcMessage.data();
        written.put(aplMessage.sender(), value);
        long count = this.written.values().stream().filter(x -> x.equals(value)).count();
        LOGGER.trace("%s WRITE count: %s/%s", aplMessage.sender(), count, threshold + 1);
        if (count > this.threshold) {
            Block block = (Block) value;
            this.written.values().stream()
                    .filter(x -> x.equals(value))
                    .map(x -> ((Block) x).getSignatures())
                    .forEach(map -> {
                        map.entrySet().forEach(entry -> {
                            block.addSignature(entry.getKey(), entry.getValue());
                        });
                    });
            LOGGER.trace("received above threshold! sending ACCEPT");
            this.written.clear();
            this.state.setVal(value);
            this.state.setValts(this.ets);
            for (BlockchainProcess p : this.processes) {
                Object valueToAccept = null;
                if (this.execMode == ExecutionMode.DIFFERENT_ACCEPT) {
                    valueToAccept = genArbitraryValue();
                    LOGGER.warn(
                            "This node is configured to use a different accept value as byzantine behaviour. Real value: %s, fake value: %s",
                            value, valueToAccept);
                }
                valueToAccept = value;
                BcMessage accept = new BcMessage(this.instanceId, BcMessageType.ACCEPT, valueToAccept);
                try {
                    p.link().send(accept);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onAccept(AuthenticatedData aplMessage, BcMessage bcMessage) {
        Object value = bcMessage.data();
        accepted.put(aplMessage.sender(), value);
        long count = this.accepted.values().stream().filter(x -> x.equals(value)).count();
        LOGGER.trace("%s ACCEPTED count: %s/%s", aplMessage.sender(), count, threshold + 1);
        if (count > this.threshold) {
            LOGGER.trace("DECIDING");
            this.accepted.clear();
            decide(value);
        }
    }

    @Override
    public void ccCollected(Collection<CcSendMessage> messages) {
        if (this.halted) {
            return;
        }
        Map<String, EpochState> states = new HashMap<>();
        for (CcSendMessage ccMessage : messages) {
            if (ccMessage.data() instanceof EpochState epochState) {
                states.put(ccMessage.sender(), epochState);
            }
        }
        Object tempVal = null;
        if (this.state.getValts() >= 0 && this.state.getVal() != null
                && bind(this.state.getValts(), this.state.getVal(), states)) {
            tempVal = this.state.getVal();
        } else if (this.state.getVal() != null && !bind(this.state.getValts(), this.state.getVal(), states)
                && states.keySet().stream().anyMatch(p -> p.equals(leader.id()))) {
            tempVal = states.get(leader.id()).getVal();
        }
        if (tempVal != null) {
            WrittenValue writtenValue = new WrittenValue(this.state.getValts(), tempVal);
            if (this.state.getWritten().contains(writtenValue)) {
                this.state.getWritten().remove(writtenValue);
            }
            if (this.execMode == ExecutionMode.DIFFERENT_WRITE) {
                Object originalVal = tempVal;
                tempVal = genArbitraryValue();
                LOGGER.warn(
                        "This node is configured to use a different write value as byzantine behaviour. Real value: %s, fake value: %s",
                        originalVal, tempVal);
            }
            Block block = (Block) tempVal;
            block.signContent(this.self.id(), this.secrets.getPrivateKey(this.self.id()));
            this.state.getWritten().add(new WrittenValue(ets, tempVal));
            for (BlockchainProcess p : this.processes) {
                BcMessage write = new BcMessage(this.instanceId, BcMessageType.WRITE, tempVal);
                try {
                    p.link().send(write);
                } catch (IOException e) {
                }
            }
        }
    }

    private boolean bind(long ts, Object val, Map<String, EpochState> states) {
        int fPlusOne = numFaults + 1;
        int count = 0;
        long highestTs = -1;
        Object highestVal = null;
        for (EpochState state : states.values()) {
            if (state.getValts() > highestTs) {
                highestTs = state.getValts();
                highestVal = state.getVal();
            }
        }
        WrittenValue highestWritten = new WrittenValue(highestTs, highestVal);
        for (EpochState state : states.values()) {
            if (state.getWritten().contains(highestWritten)) {
                count++;
            }
        }
        return highestVal != null && highestTs >= 0 && count >= fPlusOne;
    }

    public void addListener(BcListener listener) {
        this.listeners.add(listener);
    }

    public ConditionalCollect getCc() {
        return cc;
    }

    private Block genArbitraryValue() {
        return new Block("invalid", "arbitrary", new ArrayList<>());
    }

    private void complain() {
        LOGGER.warn("\n===> COMPLAIN <===\n");
    }

}
