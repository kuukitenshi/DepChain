package pt.ulisboa.tecnico.sec.depchain.common.protocol;

import java.io.Serializable;
import java.security.SignatureException;

import pt.ulisboa.tecnico.sec.depchain.common.utils.ClientSignatureData;

public abstract class Transaction implements Serializable {

    private final String sender;
    private final String receiver;
    private final long nonce;
    private final ClientSignatureData signature;

    protected Transaction(String sender, String receiver, long nonce, ClientSignatureData signature) {
        this.sender = sender;
        this.receiver = receiver;
        this.nonce = nonce;
        this.signature = signature;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public long getNonce() {
        return nonce;
    }

    public ClientSignatureData getSignature() {
        return signature;
    }

    public abstract boolean verifySignature() throws SignatureException;
}
