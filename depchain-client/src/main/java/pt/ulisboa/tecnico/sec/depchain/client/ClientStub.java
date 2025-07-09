package pt.ulisboa.tecnico.sec.depchain.client;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.web3j.crypto.ECKeyPair;

import pt.ulisboa.tecnico.sec.depchain.client.exceptions.RevertException;
import pt.ulisboa.tecnico.sec.depchain.common.info.NodeInfo;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedData;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.ContractTransaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.ExternalTransaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.Transaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.TransactionResult;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.TransactionResult.Opcode;
import pt.ulisboa.tecnico.sec.depchain.common.secrets.SecretsProvider;
import pt.ulisboa.tecnico.sec.depchain.common.utils.HexUtils;

public class ClientStub {

    private static final String BLACKLIST_CONTRACT_ADDRESS = "75dbf368dd4d6a80eac43d0f78ecdec43274721c";
    private static final String ISTCOIN_CONTRACT_ADDRESS = "39550be8da65c192dc046d8cf013174e2c94f83e";

    private final String selfAddress;
    private final ECKeyPair keyPair;
    private final Map<NodeInfo, AuthenticatedPerfectLink> nodeLinks;
    private final Map<String, TransactionResult> responses = new ConcurrentHashMap<>();
    private final int threshold;
    private final SecretsProvider secrets;

    private Transaction currentTransaction = null;
    private Transaction lastTransaction = null;

    public ClientStub(String address, ECKeyPair keyPair, Map<NodeInfo, AuthenticatedPerfectLink> nodeLinks,
            int threshold, SecretsProvider secrets) {
        this.selfAddress = address;
        this.keyPair = keyPair;
        this.nodeLinks = nodeLinks;
        this.threshold = threshold;
        this.secrets = secrets;
    }

    // ------------------------------- REPLAY ATTACK -------------------------------
    public boolean replayAttack() {
        if (this.lastTransaction == null) {
            return false;
        }
        TransactionResult result = processingReceive(this.lastTransaction);
        return true;
    }

    // ------------------------------- EXTERNAL --------------------------------
    public boolean executeExternal(String receiver, BigInteger amount) {
        ExternalTransaction transaction = ExternalTransaction.create(this.selfAddress, receiver, amount, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        return result.executed();
    }

    // ------------------------------- BLACKLIST -------------------------------
    public boolean addToBlackList(String address) throws RevertException {
        String funcSelector = Parsers.parseFuncSelector("addToBlacklist(address)");
        String paddedAddress = HexUtils.padHexStringTo256Bit(address);
        String args = Parsers.parseArguments(List.of(paddedAddress));
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, BLACKLIST_CONTRACT_ADDRESS,
                funcSelector + args, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            if (result.opcode() == Opcode.REVERT) {
                throw new RevertException();
            }
            return parseReturnDataToBoolean(result.returnData());
        }
        return false;
    }

    public boolean removeFromBlackList(String address) throws RevertException {
        String funcSelector = Parsers.parseFuncSelector("removeFromBlacklist(address)");
        String paddedAddress = HexUtils.padHexStringTo256Bit(address);
        String args = Parsers.parseArguments(List.of(paddedAddress));
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, BLACKLIST_CONTRACT_ADDRESS,
                funcSelector + args, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            if (result.opcode() == Opcode.REVERT) {
                throw new RevertException();
            }
            return parseReturnDataToBoolean(result.returnData());
        }
        return false;
    }

    public boolean isBlackListed(String address) throws RevertException {
        String funcSelector = Parsers.parseFuncSelector("isBlacklisted(address)");
        String paddedAddress = HexUtils.padHexStringTo256Bit(address);
        String args = Parsers.parseArguments(List.of(paddedAddress));
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, BLACKLIST_CONTRACT_ADDRESS,
                funcSelector + args, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            if (result.opcode() == Opcode.REVERT) {
                throw new RevertException();
            }
            return parseReturnDataToBoolean(result.returnData());
        }
        return false;
    }

    // ------------------------------- ISTCOIN -------------------------------
    public String name() {
        String funcSelector = Parsers.parseFuncSelector("name()");
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, ISTCOIN_CONTRACT_ADDRESS,
                funcSelector, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            return parseReturnDataToString(result.returnData());
        }
        return null;
    }

    public String symbol() {
        String funcSelector = Parsers.parseFuncSelector("symbol()");
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, ISTCOIN_CONTRACT_ADDRESS,
                funcSelector, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            return parseReturnDataToString(result.returnData());
        }
        return null;
    }

    public BigInteger decimals() {
        String funcSelector = Parsers.parseFuncSelector("decimals()");
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, ISTCOIN_CONTRACT_ADDRESS,
                funcSelector, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            return parseReturnDataToBigInteger(result.returnData());
        }
        return null;
    }

    public BigInteger totalSupply() {
        String funcSelector = Parsers.parseFuncSelector("totalSupply()");
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, ISTCOIN_CONTRACT_ADDRESS,
                funcSelector, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            return parseReturnDataToBigInteger(result.returnData());
        }
        return null;
    }

    public BigInteger balanceOf(String owner) {
        String funcSelector = Parsers.parseFuncSelector("balanceOf(address)");
        String paddedOwner = HexUtils.padHexStringTo256Bit(owner);
        String args = Parsers.parseArguments(List.of(paddedOwner));
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, ISTCOIN_CONTRACT_ADDRESS,
                funcSelector + args, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            return parseReturnDataToBigInteger(result.returnData());
        }
        return null;
    }

    public boolean transfer(String receiver, BigInteger amount) throws RevertException {
        String funcSelector = Parsers.parseFuncSelector("transfer(address,uint256)");
        String paddedReceiver = HexUtils.padHexStringTo256Bit(receiver);
        String args = Parsers.parseArguments(List.of(paddedReceiver, amount));
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, ISTCOIN_CONTRACT_ADDRESS,
                funcSelector + args, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            if (result.opcode() == Opcode.REVERT) {
                throw new RevertException();
            }
            return parseReturnDataToBoolean(result.returnData());
        }
        return false;
    }

    public boolean transferFrom(String sender, String receiver, BigInteger amount) throws RevertException {
        String funcSelector = Parsers.parseFuncSelector("transferFrom(address,address,uint256)");
        String paddedSender = HexUtils.padHexStringTo256Bit(sender);
        String paddedReceiver = HexUtils.padHexStringTo256Bit(receiver);
        String args = Parsers.parseArguments(List.of(paddedSender, paddedReceiver, amount));
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, ISTCOIN_CONTRACT_ADDRESS,
                funcSelector + args, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            if (result.opcode() == Opcode.REVERT) {
                throw new RevertException();
            }
            return parseReturnDataToBoolean(result.returnData());
        }
        return false;
    }

    public boolean approve(String spender, BigInteger amount) throws RevertException {
        String funcSelector = Parsers.parseFuncSelector("approve(address,uint256)");
        String paddedSpender = HexUtils.padHexStringTo256Bit(spender);
        String args = Parsers.parseArguments(List.of(paddedSpender, amount));
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, ISTCOIN_CONTRACT_ADDRESS,
                funcSelector + args, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            if (result.opcode() == Opcode.REVERT) {
                throw new RevertException();
            }
            return parseReturnDataToBoolean(result.returnData());
        }
        return false;
    }

    public BigInteger allowance(String owner, String spender) {
        String funcSelector = Parsers.parseFuncSelector("allowance(address,address)");
        String paddedOwner = HexUtils.padHexStringTo256Bit(owner);
        String paddedSpender = HexUtils.padHexStringTo256Bit(spender);
        String args = Parsers.parseArguments(List.of(paddedOwner, paddedSpender));
        ContractTransaction transaction = ContractTransaction.create(this.selfAddress, ISTCOIN_CONTRACT_ADDRESS,
                funcSelector + args, this.keyPair);
        TransactionResult result = processingReceive(transaction);
        if (result.executed()) {
            return parseReturnDataToBigInteger(result.returnData());
        }
        return null;
    }

    // -------------------------------- RETURN DATA -------------------------------
    private String parseReturnDataToString(String returnData) {
        int stringOffset = Integer.decode("0x" + returnData.substring(0, 32 * 2));
        int stringLength = Integer.decode("0x" + returnData.substring(stringOffset * 2, stringOffset * 2 + 32 * 2));
        String hexString = returnData.substring(stringOffset * 2 + 32 * 2,
                stringOffset * 2 + 32 * 2 + stringLength * 2);
        return new String(Bytes.fromHexString(hexString).toArray(), StandardCharsets.UTF_8);
    }

    private BigInteger parseReturnDataToBigInteger(String returnData) {
        return UInt256.fromHexString(returnData).toBigInteger();
    }

    private boolean parseReturnDataToBoolean(String returnData) {
        UInt256 value = UInt256.fromHexString(returnData);
        return !value.isZero();
    }

    // --------------------------------- SEND --------------------------------
    private void sendTransactionRequest(Transaction transaction) {
        System.out.println("SENDING transaction");
        for (AuthenticatedPerfectLink link : nodeLinks.values()) {
            try {
                link.send(transaction);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --------------------------------- RECIEVE QUORU -----------------
    public void onDeliver(AuthenticatedData message) {
        if (message.data() instanceof TransactionResult result) {
            synchronized (this.responses) {
                if (this.currentTransaction != null && this.currentTransaction.equals(result.transaction())) {
                    PublicKey pubNode = secrets.getPublicKey(message.sender());
                    if (result.verifySignature(pubNode)) {
                        synchronized (this.responses) {
                            this.responses.put(message.sender(), result);
                            responses.notifyAll();
                        }
                    }
                }
            }
        }
    }

    private void waitingResponses() {
        synchronized (this.responses) {
            while (this.responses.size() < this.threshold) {
                try {
                    this.responses.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(String.format("Quorum responses: %d/%d", this.responses.size(), this.threshold));
            }
        }
    }

    private TransactionResult processingReceive(Transaction transaction) {
        this.currentTransaction = transaction;
        this.lastTransaction = transaction;
        sendTransactionRequest(transaction);
        waitingResponses();
        TransactionResult result = quorumHighest();
        this.currentTransaction = null;
        this.responses.clear();
        return result;
    }

    private TransactionResult quorumHighest() {
        Map<TransactionResult, Integer> count = new HashMap<>();
        for (TransactionResult state : this.responses.values()) {
            count.putIfAbsent(state, 0);
            count.put(state, count.get(state) + 1);
        }
        TransactionResult highest = null;
        for (Map.Entry<TransactionResult, Integer> entry : count.entrySet()) {
            if (highest == null || entry.getValue() > count.get(highest)) {
                highest = entry.getKey();
            }
        }
        return highest;
    }

}
