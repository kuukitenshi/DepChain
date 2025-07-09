package pt.ulisboa.tecnico.sec.depchain.common.protocol;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;

import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;

public record TransactionResult(Transaction transaction, boolean executed, String returnData, Opcode opcode,
        byte[] signature) implements Serializable {

    public enum Opcode {
        RETURN,
        REVERT
    }

    public TransactionResult(Transaction transaction, boolean executed, String returnData, Opcode opcode,
            PrivateKey prk) {
        this(transaction, executed, returnData, opcode,
                CryptoUtils.sign(prk, transaction, executed, returnData, opcode));
    }

    public boolean verifySignature(PublicKey puk) {
        return CryptoUtils.verifySignature(puk, this.signature, this.transaction, this.executed, this.returnData,
                this.opcode);
    }

}
