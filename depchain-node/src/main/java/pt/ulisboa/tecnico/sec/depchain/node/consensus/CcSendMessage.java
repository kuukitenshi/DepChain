package pt.ulisboa.tecnico.sec.depchain.node.consensus;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;

import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;

public record CcSendMessage(long instanceId, String sender, Object data, byte[] signature) implements Serializable {

    public CcSendMessage(long instanceId, String sender, Object data, PrivateKey prk) {
        this(instanceId, sender, data, CryptoUtils.sign(prk, instanceId, sender, data));
    }

    public boolean verifySignature(PublicKey puk) {
        return CryptoUtils.verifySignature(puk, this.signature, this.instanceId, this.sender, this.data);
    }
}
