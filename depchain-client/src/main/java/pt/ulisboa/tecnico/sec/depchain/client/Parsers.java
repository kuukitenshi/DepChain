package pt.ulisboa.tecnico.sec.depchain.client;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.Hash;

import pt.ulisboa.tecnico.sec.depchain.client.hexparsing.HexParser;

public class Parsers {

    private static final HexParser HEX_PARSER = HexParser.builder().defaultAdapters().build();

    public static String parseArguments(List<Object> arguments) {
        StringBuilder sb = new StringBuilder();
        for (Object object : arguments) {
            sb.append(HEX_PARSER.toHexString(object));
        }
        return sb.toString();
    }

    public static String parseFuncSelector(String funcSelector) {
        byte[] hash = Hash.sha3(funcSelector.getBytes(StandardCharsets.UTF_8));
        return Bytes.wrap(hash).slice(0, 4).toUnprefixedHexString();
    }
}
