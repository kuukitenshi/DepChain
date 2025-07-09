package pt.ulisboa.tecnico.sec.depchain.common.net;

import java.security.Key;

import pt.ulisboa.tecnico.sec.depchain.common.utils.ByteUtils;
import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;

public record AuthenticatedData(long sequenceNumber, String sender, Object data, byte[] mac)
        implements AuthenticatedMessage {

    public AuthenticatedData(long sequenceNumber, String sender, Object data, Key key) {
        this(sequenceNumber, sender, data, calculateMac(sequenceNumber, sender, data, key));
    }

    private static byte[] calculateMac(long sequenceNumber, String sender, Object data, Key key) {
        byte[] bytes = ByteUtils.serialize(sequenceNumber, sender, data);
        return CryptoUtils.mac(key, bytes);
    }

    public boolean verifyMac(Key key) {
        byte[] bytes = ByteUtils.serialize(this.sequenceNumber, this.sender,
                this.data);
        return CryptoUtils.verifyMac(key, bytes, this.mac);
    }
}
