package pt.ulisboa.tecnico.sec.depchain.client.hexparsing.parsers;

import java.math.BigInteger;

import pt.ulisboa.tecnico.sec.depchain.client.hexparsing.HexTypeAdapter;

public class BigIntegerHexParser implements HexTypeAdapter<BigInteger>  {
    @Override
    public String toHexString(BigInteger type) {
        return String.format("%064x", type);
    }
}
