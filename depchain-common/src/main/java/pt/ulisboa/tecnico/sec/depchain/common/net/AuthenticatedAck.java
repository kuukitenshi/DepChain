package pt.ulisboa.tecnico.sec.depchain.common.net;

import java.security.Key;

import pt.ulisboa.tecnico.sec.depchain.common.utils.ByteUtils;
import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;

public record AuthenticatedAck(long sequenceNumber, byte[] mac) implements AuthenticatedMessage {

    public AuthenticatedAck(long sequenceNumber, Key key) {
        this(sequenceNumber, CryptoUtils.mac(key, ByteUtils.longToBytes(sequenceNumber)));
    }

    public boolean verifyMac(Key key) {
        byte[] bytes = ByteUtils.longToBytes(sequenceNumber);
        return CryptoUtils.verifyMac(key, bytes, this.mac);
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }
}
