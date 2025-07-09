package pt.ulisboa.tecnico.sec.depchain.client.hexparsing;

public interface HexTypeAdapter<T> {

    String toHexString(T type);
}
