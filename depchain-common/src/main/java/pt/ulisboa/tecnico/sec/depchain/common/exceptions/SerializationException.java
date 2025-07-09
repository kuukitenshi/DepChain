package pt.ulisboa.tecnico.sec.depchain.common.exceptions;

public class SerializationException extends RuntimeException {

    public SerializationException() {

    }

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(Throwable t) {
        super(t);
    }

    public SerializationException(String message, Throwable t) {
        super(message, t);
    }

}
