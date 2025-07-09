package pt.ulisboa.tecnico.sec.depchain.common.protocol;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Objects;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign.SignatureData;

import pt.ulisboa.tecnico.sec.depchain.common.utils.ClientSignatureData;
import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;

public class ExternalTransaction extends Transaction {

    private final BigInteger amount;

    private ExternalTransaction(String sender, String receiver, BigInteger amount, long nonce,
            ClientSignatureData signature) {
        super(sender, receiver, nonce, signature);
        this.amount = amount;
    }

    public static ExternalTransaction create(String sender, String receiver, BigInteger amount, ECKeyPair keyPair) {
        long nonce = CryptoUtils.genNonce();
        ClientSignatureData signature = CryptoUtils.ecSign(keyPair, sender, receiver, amount, nonce);
        return new ExternalTransaction(sender, receiver, amount, nonce, signature);
    }

    @Override
    public boolean verifySignature() throws SignatureException {
        SignatureData signatureData = getSignature().toSignatureData();
        BigInteger puk = CryptoUtils.getSignaturePubKey(signatureData, getSender(), getReceiver(), getAmount(),
                getNonce());
        return Keys.getAddress(puk).equals(getSender());
    }

    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSender(), getReceiver(), getNonce(), getSignature(), this.amount);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        ExternalTransaction other = (ExternalTransaction) obj;
        return getSender().equals(other.getSender()) && getReceiver().equals(other.getReceiver())
                && getNonce() == other.getNonce() && getSignature().equals(other.getSignature())
                && this.amount.equals(other.getAmount());
    }
}
