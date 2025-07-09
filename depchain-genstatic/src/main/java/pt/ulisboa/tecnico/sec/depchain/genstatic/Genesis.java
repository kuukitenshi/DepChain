package pt.ulisboa.tecnico.sec.depchain.genstatic;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class Genesis {

    @SerializedName("block_hash")
    public String hash = "";

    @SerializedName("previous_block_hash")
    public final String previousHash = null;

    public final List<Object> transactions = Collections.emptyList();

    public Map<String, GenesisAccount> state = new HashMap<>();

    public static class GenesisAccount {
        public BigInteger balance = BigInteger.ZERO;
    }

    public static class GenesisContractAccount extends GenesisAccount {
        public String code = "";
        public Map<String, String> storage = new HashMap<>();
    }

}
