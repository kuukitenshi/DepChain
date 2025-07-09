package pt.ulisboa.tecnico.sec.depchain.node.consensus;

import java.util.Collection;

public interface CcListener {

    void ccCollected(Collection<CcSendMessage> messages);

}
