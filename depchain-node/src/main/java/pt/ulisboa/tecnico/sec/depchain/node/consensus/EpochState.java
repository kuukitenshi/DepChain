package pt.ulisboa.tecnico.sec.depchain.node.consensus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EpochState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<WrittenValue> written = new ArrayList<>();
    private long valts = 0;
    private Object val = null;

    public EpochState() {
    }

    public EpochState(Object val) {
        this.val = val;
    }

    public List<WrittenValue> getWritten() {
        return written;
    }

    public Object getVal() {
        return this.val;
    }

    public void setVal(Object val) {
        this.val = val;
    }

    public long getValts() {
        return valts;
    }

    public void setValts(long valts) {
        this.valts = valts;
    }

    @Override
    public String toString() {
        return String.format("%s[valts=%s, val=%s, written=%s]", getClass().getSimpleName(), this.valts, val,
                Arrays.toString(written.toArray()));
    }

}
