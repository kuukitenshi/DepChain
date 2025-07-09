package pt.ulisboa.tecnico.sec.depchain.common.net;

import java.security.PrivateKey;
import java.security.PublicKey;

import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;

public record AuthenticatedHandshake(String sender, PublicKey diffiePub, byte[] signature)
        implements AuthenticatedMessage {

    public AuthenticatedHandshake(String sender, PublicKey diffiePub, PrivateKey prk) {
        this(sender, diffiePub, CryptoUtils.sign(prk, sender, diffiePub));
    }

    public boolean verifySignature(PublicKey puk) {
        return CryptoUtils.verifySignature(puk, this.signature, this.sender, this.diffiePub);
    }

    @Override
    public final String toString() {
        return String.format("%s[sender=%s]", getClass().getSimpleName(), this.sender);
    }
}
