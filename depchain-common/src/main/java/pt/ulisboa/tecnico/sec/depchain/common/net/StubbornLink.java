package pt.ulisboa.tecnico.sec.depchain.common.net;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.sec.depchain.common.utils.CancellationToken;

public class StubbornLink {

    private static final ScheduledExecutorService RESEND_SERVICE = Executors.newScheduledThreadPool(4);
    private static final long RESEND_TIME = 2000L;

    private final FairLossLink fairLossLink;

    public StubbornLink(FairLossLink fairLossLink) {
        this.fairLossLink = fairLossLink;
    }

    public CancellationToken send(Object message) throws IOException {
        CancellationToken token = new CancellationToken();
        this.fairLossLink.send(message);
        RESEND_SERVICE.schedule(() -> resendTask(message, token), RESEND_TIME, TimeUnit.MILLISECONDS);
        return token;
    }

    public void sendOnce(Object message) throws IOException {
        this.fairLossLink.send(message);
    }

    private void resendTask(Object message, CancellationToken token) {
        if (token.isCancelled()) {
            return;
        }
        try {
            this.fairLossLink.send(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        RESEND_SERVICE.schedule(() -> resendTask(message, token), RESEND_TIME, TimeUnit.MILLISECONDS);
    }
}
