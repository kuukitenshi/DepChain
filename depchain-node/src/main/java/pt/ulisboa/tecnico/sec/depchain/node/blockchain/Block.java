package pt.ulisboa.tecnico.sec.depchain.node.blockchain;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.tuweni.bytes.Bytes;

import com.google.gson.annotations.SerializedName;

import pt.ulisboa.tecnico.sec.depchain.common.protocol.Transaction;
import pt.ulisboa.tecnico.sec.depchain.common.secrets.SecretsProvider;
import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;

public class Block implements Serializable {

    @SerializedName("block_hash")
    private final String hash;

    @SerializedName("previous_hash")
    private final String previousHash;

    private final List<Transaction> transactions;

    private final Map<String, String> signatures = new HashMap<>();

    public Block(String hash, String previousHash, List<Transaction> transactions) {
        this.hash = hash;
        this.previousHash = previousHash;
        this.transactions = transactions;
    }

    public String getHash() {
        return hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public Map<String, String> getSignatures() {
        return this.signatures;
    }

    public void signContent(String node, PrivateKey prk) {
        byte[] sig = CryptoUtils.sign(prk, this.hash, this.previousHash, this.transactions);
        this.signatures.put(node, Bytes.wrap(sig).toQuantityHexString().substring(2));
    }

    public void addSignature(String node, String signature) {
        this.signatures.put(node, signature);
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(this.transactions);
    }

    public boolean isValid(SecretsProvider secrets) {
        String hash = Bytes.wrap(CryptoUtils.hash(this.transactions)).toUnprefixedHexString();
        if (!hash.equals(this.hash)) {
            return false;
        }
        for (Transaction transaction : this.transactions) {
            try {
                if (!transaction.verifySignature()) {
                    return false;
                }
            } catch (SignatureException e) {
                e.printStackTrace();
                return false;
            }
        }
        for (Map.Entry<String, String> entry : this.signatures.entrySet()) {
            PublicKey puk = secrets.getPublicKey(entry.getKey());
            byte[] signature = Bytes.fromHexString(entry.getValue()).toArray();
            if (!CryptoUtils.verifySignature(puk, signature, this.hash, this.previousHash, this.transactions)) {
                System.out.println("WRONG NODE SIGNATURE");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        Block other = (Block) obj;
        return this.hash.equals(other.hash) && this.previousHash.equals(other.previousHash)
                && this.transactions.equals(other.transactions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hash, this.previousHash, this.transactions);
    }

    @Override
    public String toString() {
        return String.format("%s[hash=%s, previousHash=%s, transactions=%s]", getClass().getSimpleName(),
                this.hash, this.previousHash, this.transactions);
    }

}
