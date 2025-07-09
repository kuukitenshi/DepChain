package pt.ulisboa.tecnico.sec.depchain.node.blockchain;

import java.util.List;
import java.util.Map;

import pt.ulisboa.tecnico.sec.depchain.common.protocol.Transaction;

public class Genesis extends Block {

    private final Map<String, Account> state;

    public Genesis(String hash, String previousHash, List<Transaction> transactions, Map<String, Account> state) {
        super(hash, previousHash, transactions);
        this.state = state;
    }

    public Map<String, Account> getState() {
        return this.state;
    }

    public Block toBlock() {
        return new Block(getHash(), getPreviousHash(), getTransactions());
    }

}
