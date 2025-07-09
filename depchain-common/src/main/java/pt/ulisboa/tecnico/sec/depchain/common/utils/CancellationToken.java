package pt.ulisboa.tecnico.sec.depchain.common.utils;

import java.util.concurrent.atomic.AtomicBoolean;

public class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public void setCancelled(boolean value) {
        this.cancelled.set(value);
    }

    public boolean isCancelled() {
        return this.cancelled.get();
    }

    public boolean checkAndCancel() {
        return this.cancelled.getAndSet(true);
    }

}
