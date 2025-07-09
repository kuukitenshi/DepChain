package pt.ulisboa.tecnico.sec.depchain.client;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import pt.ulisboa.tecnico.sec.depchain.common.info.NodeInfo;
import pt.ulisboa.tecnico.sec.depchain.common.info.SystemInfo;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.depchain.common.net.FairLossLink;
import pt.ulisboa.tecnico.sec.depchain.common.net.StubbornLink;
import pt.ulisboa.tecnico.sec.depchain.common.secrets.KeystoreSecretsProvider;
import pt.ulisboa.tecnico.sec.depchain.common.secrets.SecretsProvider;

public class DepchainClient {

    private static final String LOGO = """
            ████████▄     ▄████████    ▄███████▄  ▄████████    ▄█    █▄       ▄████████  ▄█  ███▄▄▄▄
            ███   ▀███   ███    ███   ███    ███ ███    ███   ███    ███     ███    ███ ███  ███▀▀▀██▄
            ███    ███   ███    █▀    ███    ███ ███    █▀    ███    ███     ███    ███ ███▌ ███   ███
            ███    ███  ▄███▄▄▄       ███    ███ ███         ▄███▄▄▄▄███▄▄   ███    ███ ███▌ ███   ███
            ███    ███ ▀▀███▀▀▀     ▀█████████▀  ███        ▀▀███▀▀▀▀███▀  ▀███████████ ███▌ ███   ███
            ███    ███   ███    █▄    ███        ███    █▄    ███    ███     ███    ███ ███  ███   ███
            ███   ▄███   ███    ███   ███        ███    ███   ███    ███     ███    ███ ███  ███   ███
            ████████▀    ██████████  ▄████▀      ████████▀    ███    █▀      ███    █▀  █▀    ▀█   █▀
                        """;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Required arguments: <client key> <members file> <keystore>");
            System.exit(1);
        }
        SystemInfo systemInfo = SystemInfo.load(new File(args[1]));
        SecretsProvider secrets;
        try {
            secrets = new KeystoreSecretsProvider(new File(args[2]), "secgroup03".toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            System.err.println("failed to load keystore!");
            System.exit(1);
            return;
        }
        String clientKey = args[0];
        ECKeyPair keyPair;
        try {
            keyPair = loadKeyPair(clientKey);
        } catch (IOException e) {
            System.err.println("Failed to load keypair!");
            e.printStackTrace();
            System.exit(1);
            return;
        }
        System.out.println(LOGO);
        String address = Keys.getAddress(keyPair);
        System.out.println("Your address: " + address);

        int n = systemInfo.nodes().size();
        int numFaults = (int) ((n - 1) / 3.0);
        System.out.println("f = " + numFaults);
        int threshold = 2 * numFaults + 1;

        try (DatagramSocket socket = new DatagramSocket()) {
            Map<NodeInfo, AuthenticatedPerfectLink> nodeLinks = new HashMap<>();
            systemInfo.nodes().forEach(node -> {
                PublicKey puk = secrets.getPublicKey(node.id());
                KeyPair pair = new KeyPair(puk, null);
                FairLossLink fll = new FairLossLink(socket, node.clientAddress());
                StubbornLink sl = new StubbornLink(fll);
                AuthenticatedPerfectLink apl = new AuthenticatedPerfectLink(sl, "client", node.id(), pair);
                try {
                    apl.startHandshake();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                nodeLinks.put(node, apl);
            });
            ClientStub clientStub = new ClientStub(address, keyPair, nodeLinks, threshold, secrets);
            SocketThread st = new SocketThread(socket, nodeLinks, clientStub);
            st.start();
            try (Scanner sc = new Scanner(System.in)) {
                Menu menu = new Menu(sc, clientStub);
                while (true) {
                    menu.execMenu();
                }
            }
        } catch (IOException e) {
            System.err.println("failed to create socket!");
            System.exit(1);
            return;
        }
    }

    private static ECKeyPair loadKeyPair(String keyPath) throws IOException {
        byte[] bytes = Files.readAllBytes(Path.of(keyPath));
        return Keys.deserialize(bytes);
    }

}
