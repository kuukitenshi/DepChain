package pt.ulisboa.tecnico.sec.depchain.node.consensus;

public interface BcListener {

    void bcDecided(Object value);

    void bcAborted(EpochState state);

}
