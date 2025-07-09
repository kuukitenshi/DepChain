package pt.ulisboa.tecnico.sec.depchain.node.consensus;

import java.io.Serializable;
import java.util.Map;

public record CcCollectMessage(long instanceId, Map<String, CcSendMessage> messages) implements Serializable {

}
