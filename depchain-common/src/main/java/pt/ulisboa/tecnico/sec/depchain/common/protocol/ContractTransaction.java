package pt.ulisboa.tecnico.sec.depchain.common.protocol;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Objects;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign.SignatureData;

import pt.ulisboa.tecnico.sec.depchain.common.utils.ClientSignatureData;
import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;

public class ContractTransaction extends Transaction {

    private final String callData;

    private ContractTransaction(String sender, String receiver, String callData, long nonce,
            ClientSignatureData signature) {
        super(sender, receiver, nonce, signature);
        this.callData = callData;
    }

    public static ContractTransaction create(String sender, String receiver, String callData, ECKeyPair keyPair) {
        long nonce = CryptoUtils.genNonce();
        ClientSignatureData signature = CryptoUtils.ecSign(keyPair, sender, receiver, callData, nonce);
        return new ContractTransaction(sender, receiver, callData, nonce, signature);
    }

    @Override
    public boolean verifySignature() throws SignatureException {
        SignatureData signatureData = getSignature().toSignatureData();
        BigInteger puk = CryptoUtils.getSignaturePubKey(signatureData, getSender(), getReceiver(), getCallData(),
                getNonce());
        return Keys.getAddress(puk).equals(getSender());
    }

    public String getCallData() {
        return callData;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSender(), getReceiver(), getNonce(), getSignature(), this.callData);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        ContractTransaction other = (ContractTransaction) obj;
        return getSender().equals(other.getSender()) && getReceiver().equals(other.getReceiver())
                && getNonce() == other.getNonce() && getSignature().equals(other.getSignature())
                && this.callData.equals(other.callData);
    }
}
