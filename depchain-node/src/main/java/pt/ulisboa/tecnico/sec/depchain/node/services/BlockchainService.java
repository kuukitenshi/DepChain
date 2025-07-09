package pt.ulisboa.tecnico.sec.depchain.node.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import org.apache.tuweni.bytes.Bytes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.ulisboa.tecnico.sec.depchain.common.logging.Logger;
import pt.ulisboa.tecnico.sec.depchain.common.logging.LoggerFactory;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.ContractTransaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.ExternalTransaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.Transaction;
import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.Account;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.AccountDeserializer;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.Block;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.Blockchain;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.Genesis;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.TransactionDeserializer;
import pt.ulisboa.tecnico.sec.depchain.node.container.ServiceHandle;
import pt.ulisboa.tecnico.sec.depchain.node.services.communication.ConsensusDecided;
import pt.ulisboa.tecnico.sec.depchain.node.services.communication.StartConsensus;

public class BlockchainService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockchainService.class);
    private static final int TRANSACTIONS_PER_BLOCK = 1;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Account.class, new AccountDeserializer())
            .registerTypeAdapter(Transaction.class, new TransactionDeserializer())
            .create();

    private final Blockchain blockchain = new Blockchain();
    private final Queue<Transaction> queuedTransactions = new ArrayDeque<>();
    private final ServiceHandle<BlockchainService> handle;

    public BlockchainService(ServiceHandle<BlockchainService> handle) {
        this.handle = handle;
        handle.listens(ConsensusDecided.class, this::onDecided);
        handle.listens(ExternalTransaction.class, this::onTransaction);
        handle.listens(ContractTransaction.class, this::onTransaction);
        handle.publishes(StartConsensus.class);
    }

    public void init(Genesis genesis) {
        this.blockchain.append(genesis.toBlock());
        try {
            loadBlocks();
        } catch (IOException e) {
            LOGGER.error("Failed to load blocks from disk!");
            e.printStackTrace();
        }
    }

    private void onTransaction(Transaction transaction) {
        this.queuedTransactions.add(transaction);
        if (this.queuedTransactions.size() == TRANSACTIONS_PER_BLOCK) {
            LOGGER.info("Creating new block...");
            List<Transaction> transactions = new ArrayList<>(this.queuedTransactions);
            String hash = Bytes.wrap(CryptoUtils.hash(transaction)).toUnprefixedHexString();
            String previousHash = this.blockchain.getLast().getHash();
            Block block = new Block(hash, previousHash, transactions);
            this.handle.publish(new StartConsensus(block));
            this.queuedTransactions.clear();
        }
    }

    private void onDecided(ConsensusDecided consensus) {
        if (consensus.value() instanceof Block block) {
            this.blockchain.append(block);
            LOGGER.info("appended new block to the blockchain!");
            try {
                persistBlock(block);
            } catch (IOException e) {
                LOGGER.error("Failed to persist block!");
                e.printStackTrace();
            }
        }
    }

    private void persistBlock(Block block) throws IOException {
        String json = GSON.toJson(block);
        File file = Path.of("blocks", block.getHash() + ".json").toFile();
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json);
            LOGGER.info("block persisted!");
        }
    }

    private void loadBlocks() throws IOException {
        HashMap<String, Block> hashBlock = new HashMap<>();
        Path folderBlocks = Path.of("blocks");
        if (!Files.exists(folderBlocks)) {
            return;
        }
        for (Path path : Files.list(folderBlocks).toList()) {
            String json = Files.readString(path);
            Block block = GSON.fromJson(json, Block.class);
            hashBlock.put(block.getPreviousHash(), block);
        }
        while (hashBlock.size() > 0) {
            String lastHash = blockchain.getLast().getHash();
            Block block = hashBlock.get(lastHash);
            this.blockchain.append(block);
            hashBlock.remove(lastHash);
        }
    }

}
