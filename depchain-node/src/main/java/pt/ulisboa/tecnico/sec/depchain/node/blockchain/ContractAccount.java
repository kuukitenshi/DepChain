package pt.ulisboa.tecnico.sec.depchain.node.blockchain;

import java.math.BigInteger;
import java.util.Map;

public class ContractAccount extends Account {

    private String code;
    private Map<String, String> storage;

    public ContractAccount(BigInteger balance, String code, Map<String, String> storage) {
        super(balance);
        this.code = code;
        this.storage = storage;
    }

    public String getCode() {
        return this.code;
    }

    public Map<String, String> getStorage() {
        return this.storage;
    }

}
