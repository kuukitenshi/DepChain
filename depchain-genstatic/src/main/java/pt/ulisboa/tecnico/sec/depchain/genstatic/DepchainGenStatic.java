package pt.ulisboa.tecnico.sec.depchain.genstatic;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.ulisboa.tecnico.sec.depchain.common.utils.CryptoUtils;
import pt.ulisboa.tecnico.sec.depchain.common.utils.HexUtils;
import pt.ulisboa.tecnico.sec.depchain.genstatic.Genesis.GenesisAccount;
import pt.ulisboa.tecnico.sec.depchain.genstatic.Genesis.GenesisContractAccount;

public class DepchainGenStatic {

    private static final String BLACKLIST_ADDRESS = "75dbf368dd4d6a80eac43d0f78ecdec43274721c";
    private static final String IST_COIN_ADDRESS = "39550be8da65c192dc046d8cf013174e2c94f83e";

    private static final Path BLACKLIST_PATH = Path.of("smart-contracts", "AccessControl.bytecode");
    private static final Path IST_COIN_PATH = Path.of("smart-contracts", "ISTCoin.bytecode");

    private static final String DOMAIN = "CN=, OU=, O=, L=, ST=, C=, EMAILADDRESS=";
    private static final String KEYSTORE_PASS = "secgroup03";
    private static final BigInteger INITIAL_BALANCE = BigInteger.valueOf(10000);

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println(
                    "Invalid arguments, use: <nr blockchain members> <nr clients> <keystore path> <members file path>");
            System.exit(1);
            return;
        }
        int nrMembers = Integer.parseInt(args[0]);
        int nrClients = Integer.parseInt(args[1]);
        String keyStorePath = args[2];
        String membersPath = args[3];

        System.out.println(String.format("generating keys for %s blockchain members", nrMembers));
        generateKeystore(keyStorePath, nrMembers);

        System.out.println("generating members file");
        generateMembersFile(nrMembers, membersPath);

        List<ECKeyPair> keyPairs = new ArrayList<>();
        System.out.println(String.format("generating EC keys for %s clients", nrClients));
        for (int i = 0; i < nrClients; i++) {
            ECKeyPair keyPair = generateECKeypair("client-" + i);
            keyPairs.add(keyPair);
        }

        System.out.println("creating genesis block");
        generateGenesis(keyPairs);

        System.out.println("static info generated!");
    }

    private static void generateKeystore(String keystorePath, int nrMembers) throws IOException, InterruptedException {
        for (int i = 0; i < nrMembers; i++) {
            List<String> commandParameters = new ArrayList<>(List.of("keytool", "-genkey"));
            commandParameters.addAll(List.of("-alias", "node-" + i));
            commandParameters.addAll(List.of("-keyalg", "RSA"));
            commandParameters.addAll(List.of("-keysize", "2048"));
            commandParameters.addAll(List.of("-keystore", keystorePath));
            commandParameters.addAll(List.of("-storepass", KEYSTORE_PASS));
            commandParameters.addAll(List.of("-dname", DOMAIN));
            commandParameters.addAll(List.of("-validity", "365"));
            Process proc = new ProcessBuilder(commandParameters).start();
            proc.waitFor();
            String stdout = proc.inputReader().lines().collect(Collectors.joining());
            String stderr = proc.errorReader().lines().collect(Collectors.joining());
            if (!stdout.isBlank()) {
                System.out.println(stdout);
            }
            if (!stderr.isBlank()) {
                System.out.println(stderr);
            }
        }
    }

    private static void generateMembersFile(int nrMembers, String membersPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(membersPath))) {
            writer.println("[nodes]");
            int baseNodePort = 7000;
            int baseClientPort = 8000;
            for (int i = 0; i < nrMembers; i++) {
                writer.println();
                writer.println(String.format("[nodes.node-%s]", i));
                writer.println("hostname = \"localhost\"");
                writer.println("node_port = " + baseNodePort++);
                writer.println("client_port = " + baseClientPort++);
            }
        }
    }

    private static ECKeyPair generateECKeypair(String alias) throws IOException {
        ECKeyPair keyPair;
        try {
            keyPair = Keys.createEcKeyPair();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
        byte[] bytes = Keys.serialize(keyPair);
        Files.write(Path.of(alias + ".key"), bytes);
        return keyPair;
    }

    private static void generateGenesis(List<ECKeyPair> clients) throws IOException {
        Genesis genesis = new Genesis();
        String client0 = null;
        for (ECKeyPair keyPair : clients) {
            String address = Keys.getAddress(keyPair);
            GenesisAccount account = new GenesisAccount();
            account.balance = INITIAL_BALANCE;
            genesis.state.put(address, account);
            if (client0 == null) {
                client0 = address;
                System.out.println("Client 0: " + client0);
            }
        }
        byte[] hash = CryptoUtils.hash(genesis.transactions);
        genesis.hash = Bytes.wrap(hash).toUnprefixedHexString();
        GenesisContractAccount blacklist = new GenesisContractAccount();
        blacklist.code = getContractBytecode(BLACKLIST_PATH);
        String admin = getSlotMapping(client0, 1);
        blacklist.storage.put(admin, "1");
        genesis.state.put(BLACKLIST_ADDRESS, blacklist);

        GenesisContractAccount istcoin = new GenesisContractAccount();
        istcoin.code = getContractBytecode(IST_COIN_PATH);
        String totalSupply = UInt256.valueOf(100_000_000).toQuantityHexString().substring(2);

        String balance = getSlotMapping(client0, 0);
        istcoin.storage.put(balance, totalSupply);
        istcoin.storage.put("2", totalSupply);
        istcoin.storage.put("5", BLACKLIST_ADDRESS);
        genesis.state.put(IST_COIN_ADDRESS, istcoin);

        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        String json = gson.toJson(genesis);
        try (FileWriter writer = new FileWriter("genesis.json")) {
            writer.write(json);
        }
    }

    private static String getContractBytecode(Path path) throws IOException {
        return Files.readString(path).trim();
    }

    private static String getSlotMapping(String address, int key) {
        String paddedAddress = HexUtils.padHexStringTo256Bit(address);
        String stateVariableIndex = HexUtils.convertIntegerToHex256Bit(key);
        return Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(paddedAddress + stateVariableIndex)));
    }

}
