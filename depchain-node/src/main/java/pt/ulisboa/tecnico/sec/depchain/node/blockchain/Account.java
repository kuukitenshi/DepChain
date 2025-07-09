package pt.ulisboa.tecnico.sec.depchain.node.blockchain;

import java.math.BigInteger;

public abstract class Account {

    private BigInteger balance;

    public Account(BigInteger balance) {
        this.balance = balance;
    }

    public BigInteger getBalance() {
        return this.balance;
    }

}
