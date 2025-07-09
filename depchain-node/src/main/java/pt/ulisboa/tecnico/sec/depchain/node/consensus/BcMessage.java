package pt.ulisboa.tecnico.sec.depchain.node.consensus;

import java.io.Serializable;

public record BcMessage(long instanceId, BcMessageType type, Object data) implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum BcMessageType {
        READ,
        WRITE,
        ACCEPT
    }

}
