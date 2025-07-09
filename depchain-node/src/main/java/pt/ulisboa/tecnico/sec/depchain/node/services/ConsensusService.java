package pt.ulisboa.tecnico.sec.depchain.node.services;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import pt.ulisboa.tecnico.sec.depchain.common.logging.Logger;
import pt.ulisboa.tecnico.sec.depchain.common.logging.LoggerFactory;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedData;
import pt.ulisboa.tecnico.sec.depchain.common.utils.StorageBuffer;
import pt.ulisboa.tecnico.sec.depchain.node.BlockchainProcess;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.Block;
import pt.ulisboa.tecnico.sec.depchain.node.command_line.CommandLineArguments.ExecutionMode;
import pt.ulisboa.tecnico.sec.depchain.node.consensus.BcListener;
import pt.ulisboa.tecnico.sec.depchain.node.consensus.BcMessage;
import pt.ulisboa.tecnico.sec.depchain.node.consensus.ByzantineConsensus;
import pt.ulisboa.tecnico.sec.depchain.node.consensus.CcCollectMessage;
import pt.ulisboa.tecnico.sec.depchain.node.consensus.CcSendMessage;
import pt.ulisboa.tecnico.sec.depchain.node.consensus.EpochState;
import pt.ulisboa.tecnico.sec.depchain.node.container.ServiceHandle;
import pt.ulisboa.tecnico.sec.depchain.node.services.communication.ConsensusAborted;
import pt.ulisboa.tecnico.sec.depchain.node.services.communication.ConsensusDecided;
import pt.ulisboa.tecnico.sec.depchain.node.services.communication.StartConsensus;
import pt.ulisboa.tecnico.sec.depchain.node.services.configs.GlobalServiceConfig;

public class ConsensusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsensusService.class);

    private final Map<Long, ByzantineConsensus> consensus = new HashMap<>();
    private final Map<Long, StorageBuffer<AuthenticatedData>> messageBuffer = new HashMap<>();
    private final ServiceHandle<ConsensusService> handle;

    private long instanceCounter = 0;

    private GlobalServiceConfig config;

    public ConsensusService(ServiceHandle<ConsensusService> handle) {
        this.handle = handle;
        handle.publishes(ConsensusDecided.class);
        handle.publishes(ConsensusAborted.class);
        handle.listens(StartConsensus.class, this::onStartConsensus);
        handle.listens(AuthenticatedData.class, this::onMessage);
    }

    public void init(GlobalServiceConfig config) {
        this.config = config;
        if (config.execMode() == ExecutionMode.DIFFERENT_CLIENT) {
            LOGGER.warn("This node is configured to use submit different client requests as byzantine behaviour.");
            LOGGER.warn("It will use a different string than the one request in append by the client.");
        }
    }

    private void onStartConsensus(StartConsensus start) {
        Block block = start.block();
        BlockchainProcess leader = this.config.processes().values().stream().filter(x -> x.id().equals("node-0"))
                .findFirst().get();
        ByzantineConsensus consensus = createConsensusInstance(this.instanceCounter, block);
        this.instanceCounter++;
        if (leader.equals(this.config.self())) {
            try {
                consensus.propose(block);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onMessage(AuthenticatedData message) {
        Long id = null;
        if (message.data() instanceof BcMessage bcMessage) {
            id = bcMessage.instanceId();
            if (this.consensus.containsKey(id)) {
                this.consensus.get(id).deliver(message);
                return;
            }
        } else if (message.data() instanceof CcSendMessage ccMessage) {
            id = ccMessage.instanceId();
            if (this.consensus.containsKey(id)) {
                this.consensus.get(id).getCc().deliver(message);
                return;
            }
        } else if (message.data() instanceof CcCollectMessage ccMessage) {
            id = ccMessage.instanceId();
            if (this.consensus.containsKey(id)) {
                this.consensus.get(id).getCc().deliver(message);
                return;
            }
        }
        if (id == null) {
            return;
        }
        LOGGER.trace("storing message in buffer %s", id);
        this.messageBuffer.putIfAbsent(id, new StorageBuffer<>());
        this.messageBuffer.get(id).store(message);
    }

    private ByzantineConsensus createConsensusInstance(long id, Object initialValue) {
        BlockchainProcess leader = this.config.processes().values().stream().filter(x -> x.id().equals("node-0"))
                .findFirst().get();
        EpochState state = new EpochState(initialValue);
        ByzantineConsensus consesus = new ByzantineConsensus(id,
                this.config.processes().values(), this.config.self(),
                leader, this.config.numFaults(), state, this.config.secrets(), this.config.execMode(), this::predicate);
        consesus.addListener(new BcListener() {
            @Override
            public void bcDecided(Object value) {
                handle.publish(new ConsensusDecided(id, value));
            }

            @Override
            public void bcAborted(EpochState state) {
                handle.publish(new ConsensusAborted(id));
            }
        });
        this.consensus.put(id, consesus);
        if (this.messageBuffer.containsKey(id)) {
            LOGGER.trace("dumping messages from buffer");
            this.messageBuffer.get(id).dump(this::onMessage);
            this.messageBuffer.remove(id);
        }
        LOGGER.info("created byzantine consensus instance %s", id);
        return consesus;
    }

    private boolean predicate(Collection<CcSendMessage> messages) {
        for (CcSendMessage message : messages) {
            if (message.data() instanceof Block block && !block.isValid(this.config.secrets())) {
                LOGGER.warn("Invalid block!");
                return false;
            }
        }
        return true;
    }

}
