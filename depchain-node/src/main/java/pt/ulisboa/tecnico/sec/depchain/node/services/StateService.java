package pt.ulisboa.tecnico.sec.depchain.node.services;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ulisboa.tecnico.sec.depchain.common.logging.Logger;
import pt.ulisboa.tecnico.sec.depchain.common.logging.LoggerFactory;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.ContractTransaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.ExternalTransaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.Transaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.TransactionResult;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.TransactionResult.Opcode;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.Account;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.AccountDeserializer;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.Block;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.ContractAccount;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.ExternalAccount;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.Genesis;
import pt.ulisboa.tecnico.sec.depchain.node.container.Pair;
import pt.ulisboa.tecnico.sec.depchain.node.container.ServiceHandle;
import pt.ulisboa.tecnico.sec.depchain.node.services.communication.ConsensusDecided;
import pt.ulisboa.tecnico.sec.depchain.node.services.configs.GlobalServiceConfig;

public class StateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateService.class);
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Account.class, new AccountDeserializer())
            .create();

    private final SimpleWorld world = new SimpleWorld();
    private final ServiceHandle<StateService> handle;

    private GlobalServiceConfig gConfig;

    public StateService(ServiceHandle<StateService> handle) {
        this.handle = handle;
        handle.listens(ConsensusDecided.class, this::onDecided);
        handle.publishes(TransactionResult.class);
    }

    public void init(GlobalServiceConfig gConfig, Genesis genesis) {
        this.gConfig = gConfig;
        Path path = Path.of("state.json");
        if (Files.exists(path)) {
            try {
                Map<String, Account> state = loadStateFromFile(path);
                setWorlState(state);
                LOGGER.info("state loaded from file");
            } catch (IOException e) {
                LOGGER.error("failed to load state from file!");
                e.printStackTrace();
            }
        } else {
            setWorlState(genesis.getState());
            LOGGER.info("state loaded from genesis");
            persistState();
        }
    }

    private void onDecided(ConsensusDecided consensus) {
        if (consensus.value() instanceof Block block) {
            LOGGER.trace("executing transactions");
            for (Transaction transaction : block.getTransactions()) {
                executeTransaction(transaction);
            }
            persistState();
        }
    }

    private void executeTransaction(Transaction transaction) {
        boolean executed = false;
        String returnData = null;
        Opcode opcode = Opcode.RETURN;
        try {
            Address sender = Address.fromHexString(transaction.getSender());
            Address receiver = Address.fromHexString(transaction.getReceiver());
            if (transaction instanceof ExternalTransaction external) {
                Wei amount = Wei.of(external.getAmount());
                MutableAccount senderAccount = this.world.getAccount(sender);
                MutableAccount receiverAccount = this.world.getAccount(receiver);
                if (senderAccount != null && receiverAccount != null
                        && senderAccount.getBalance().greaterOrEqualThan(amount)) {
                    senderAccount.decrementBalance(amount);
                    receiverAccount.incrementBalance(amount);
                    this.world.commit();
                    executed = true;
                }
            } else if (transaction instanceof ContractTransaction contract) {
                Address smartContract = Address.fromHexString(contract.getReceiver());
                MutableAccount contractAccount = this.world.getAccount(smartContract);
                if (this.world.get(sender) != null && contractAccount != null && contractAccount.hasCode()) {
                    Bytes code = contractAccount.getCode();
                    Bytes callData = Bytes.fromHexString(contract.getCallData());
                    Pair<String, Opcode> result = executeEVM(code, sender, smartContract, callData);
                    returnData = result.first();
                    opcode = result.second();
                    executed = true;
                }
            }
        } catch (Exception e) {
        }
        String self = this.gConfig.self().id();
        PrivateKey prk = this.gConfig.secrets().getPrivateKey(self);
        TransactionResult result = new TransactionResult(transaction, executed, returnData, opcode, prk);
        this.handle.publish(result);
    }

    private void persistState() {
        Map<String, Account> state = new HashMap<>();
        world.getTouchedAccounts().forEach(account -> {
            Address address = account.getAddress();
            BigInteger balance = account.getBalance().toBigInteger();
            if (account.hasCode()) {
                String code = account.getCode().toUnprefixedHexString();
                Map<String, String> storage = new HashMap<>();
                MutableAccount worldAccount = world.getAccount(address);
                for (Map.Entry<UInt256, UInt256> entry : worldAccount.getUpdatedStorage().entrySet()) {
                    String key = entry.getKey().toQuantityHexString().substring(2);
                    String value = entry.getValue().toQuantityHexString().substring(2);
                    storage.put(key, value);
                }
                ContractAccount contract = new ContractAccount(balance, code, storage);
                state.put(address.toUnprefixedHexString(), contract);
            } else {
                ExternalAccount external = new ExternalAccount(balance);
                state.put(address.toUnprefixedHexString(), external);
            }
        });
        String json = GSON.toJson(state);
        try (FileWriter writer = new FileWriter("state.json")) {
            writer.write(json);
            LOGGER.info("state persisted!");
        } catch (IOException e) {
            LOGGER.error("Failed to save state to file!");
        }
    }

    private Map<String, Account> loadStateFromFile(Path path) throws IOException {
        String json = Files.readString(path);
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        Map<String, Account> state = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            JsonElement element = jsonObject.get(key);
            Account account = GSON.fromJson(element, Account.class);
            state.put(key, account);
        }
        return state;
    }

    private void setWorlState(Map<String, Account> state) {
        for (Map.Entry<String, Account> entry : state.entrySet()) {
            String addressStr = entry.getKey();
            Account account = entry.getValue();
            if (account instanceof ExternalAccount external) {
                Address address = Address.fromHexString(addressStr);
                Wei initialBalance = Wei.of(external.getBalance());
                this.world.createAccount(address, 0, initialBalance);
            } else if (account instanceof ContractAccount contract) {
                Address address = Address.fromHexString(addressStr);
                Wei initialBalance = Wei.of(contract.getBalance());
                MutableAccount createdAccount = this.world.createAccount(address, 0, initialBalance);
                Bytes code = Bytes.fromHexStringLenient(contract.getCode());
                createdAccount.setCode(code);
                for (Map.Entry<String, String> storageEntry : contract.getStorage().entrySet()) {
                    UInt256 key = UInt256.fromHexString(storageEntry.getKey());
                    UInt256 value = UInt256.fromHexString(storageEntry.getValue());
                    createdAccount.setStorageValue(key, value);
                }
            }
        }
        this.world.commit();
    }

    private Pair<String, Opcode> executeEVM(Bytes code, Address sender, Address receiver, Bytes callData) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);
        EVMExecutor evm = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        evm.tracer(tracer);
        evm.worldUpdater(this.world.updater());
        evm.commitWorldState();
        evm.code(code);
        evm.sender(sender);
        evm.receiver(receiver);
        evm.callData(callData);
        evm.execute();
        return extractReturnData(baos);
    }

    private Pair<String, Opcode> extractReturnData(ByteArrayOutputStream baos) {
        String[] lines = baos.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
        String memory = jsonObject.get("memory").getAsString();
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());
        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        Opcode opcode = Opcode.valueOf(jsonObject.get("opName").getAsString());
        return new Pair<>(returnData, opcode);
    }

}
