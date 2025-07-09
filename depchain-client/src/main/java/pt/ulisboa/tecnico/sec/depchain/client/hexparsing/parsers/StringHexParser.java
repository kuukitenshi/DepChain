package pt.ulisboa.tecnico.sec.depchain.client.hexparsing.parsers;

import pt.ulisboa.tecnico.sec.depchain.client.hexparsing.HexTypeAdapter;

public class StringHexParser implements HexTypeAdapter<String> {
    @Override
    public String toHexString(String type) {
        return type;
    }
}
