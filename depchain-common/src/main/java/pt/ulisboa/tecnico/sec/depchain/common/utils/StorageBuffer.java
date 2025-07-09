package pt.ulisboa.tecnico.sec.depchain.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StorageBuffer<T> {

    private final List<T> buffer = new ArrayList<>();

    public void store(T value) {
        this.buffer.add(value);
    }

    public void dump(Consumer<T> consumer) {
        this.buffer.forEach(consumer::accept);
        this.buffer.clear();
    }

}
