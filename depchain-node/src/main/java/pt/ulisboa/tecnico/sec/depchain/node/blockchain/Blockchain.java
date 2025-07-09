package pt.ulisboa.tecnico.sec.depchain.node.blockchain;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Blockchain {

    private final LinkedList<Block> chain = new LinkedList<>();

    public void append(Block block) {
        this.chain.addFirst(block);
    }

    public List<Block> getChain() {
        return Collections.unmodifiableList(this.chain);
    }

    public Block getLast() {
        return this.chain.getFirst();
    }

}
