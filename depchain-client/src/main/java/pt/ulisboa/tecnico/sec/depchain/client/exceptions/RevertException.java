package pt.ulisboa.tecnico.sec.depchain.client.exceptions;

public class RevertException extends RuntimeException {

    public RevertException() {
    }

    public RevertException(String message) {
        super(message);
    }

    public RevertException(Throwable t) {
        super(t);
    }

    public RevertException(String message, Throwable t) {
        super(message, t);
    }
    
}
