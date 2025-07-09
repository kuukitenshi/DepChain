package pt.ulisboa.tecnico.sec.depchain.common.net;

import java.security.Key;

import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;

public record AuthenticatedHandshakeAck(String sender, byte[] mac) implements AuthenticatedMessage {

    public AuthenticatedHandshakeAck(String sender, Key key) {
        this(sender, CryptoUtils.mac(key, sender.getBytes()));
    }

    public boolean verifyMac(Key key) {
        return CryptoUtils.verifyMac(key, this.sender.getBytes(), this.mac);
    }
}
