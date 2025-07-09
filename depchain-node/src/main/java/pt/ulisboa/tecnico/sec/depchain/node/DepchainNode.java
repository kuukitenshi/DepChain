package pt.ulisboa.tecnico.sec.depchain.node;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.ulisboa.tecnico.sec.depchain.common.info.NodeInfo;
import pt.ulisboa.tecnico.sec.depchain.common.logging.Logger;
import pt.ulisboa.tecnico.sec.depchain.common.logging.LoggerFactory;
import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.depchain.common.net.FairLossLink;
import pt.ulisboa.tecnico.sec.depchain.common.net.StubbornLink;
import pt.ulisboa.tecnico.sec.depchain.common.secrets.KeystoreSecretsProvider;
import pt.ulisboa.tecnico.sec.depchain.common.secrets.SecretsProvider;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.Account;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.AccountDeserializer;
import pt.ulisboa.tecnico.sec.depchain.node.blockchain.Genesis;
import pt.ulisboa.tecnico.sec.depchain.node.command_line.CommandLineArguments;
import pt.ulisboa.tecnico.sec.depchain.node.command_line.CommandLineException;
import pt.ulisboa.tecnico.sec.depchain.node.command_line.CommandLineArguments.ExecutionMode;
import pt.ulisboa.tecnico.sec.depchain.node.container.ServiceContainer;
import pt.ulisboa.tecnico.sec.depchain.node.services.BlockchainService;
import pt.ulisboa.tecnico.sec.depchain.node.services.ClientService;
import pt.ulisboa.tecnico.sec.depchain.node.services.ConsensusService;
import pt.ulisboa.tecnico.sec.depchain.node.services.NodeService;
import pt.ulisboa.tecnico.sec.depchain.node.services.StateService;
import pt.ulisboa.tecnico.sec.depchain.node.services.configs.ClientServiceConfig;
import pt.ulisboa.tecnico.sec.depchain.node.services.configs.GlobalServiceConfig;
import pt.ulisboa.tecnico.sec.depchain.node.services.configs.NodeServiceConfig;

public class DepchainNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(DepchainNode.class);
    private static final String LOGO = """
             _____               ______ __           __
            |     \\.-----.-----.|      |  |--.---.-.|__|.-----.
            |  --  |  -__|  _  ||   ---|     |  _  ||  ||     |
            |_____/|_____|   __||______|__|__|___._||__||__|__|
                         |__|
                        """;

    public static void main(String[] args) {
        CommandLineArguments cArgs;
        try {
            cArgs = CommandLineArguments.parse(args);
        } catch (CommandLineException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }
        if (cArgs.executionMode() == ExecutionMode.CRASH) {
            LOGGER.warn("Due to the defined byzantine mode, this process will terminate in 1 minmute.");
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    LOGGER.warn("Crashing...");
                    System.exit(0);
                }
            }, 60000L);
        }
        NodeInfo selfNode = cArgs.self();
        SecretsProvider secrets = new KeystoreSecretsProvider(cArgs.keyStore(), "secgroup03".toCharArray());
        PrivateKey prk = secrets.getPrivateKey(selfNode.id());
        Map<SocketAddress, BlockchainProcess> processes = new HashMap<>();
        DatagramSocket nodeSocket;
        DatagramSocket clientSocket;
        try {
            nodeSocket = new DatagramSocket(selfNode.nodePort());
            clientSocket = new DatagramSocket(selfNode.clientPort());
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        BlockchainProcess self = null;
        for (NodeInfo node : cArgs.systemInfo().nodes()) {
            PublicKey puk = secrets.getPublicKey(node.id());
            KeyPair pair = new KeyPair(puk, prk);
            FairLossLink fll = new FairLossLink(nodeSocket, node.nodeAddress());
            StubbornLink sl = new StubbornLink(fll);
            AuthenticatedPerfectLink apl = new AuthenticatedPerfectLink(sl, selfNode.id(), node.id(), pair);
            BlockchainProcess process = new BlockchainProcess(node.id(), apl);
            processes.put(node.nodeAddress(), process);
            if (node == selfNode) {
                self = process;
            }
        }
        int n = cArgs.systemInfo().nodes().size();
        int numFaults = (int) ((n - 1) / 3.0);
        GlobalServiceConfig globalConfig = new GlobalServiceConfig(processes, self, secrets, numFaults,
                cArgs.executionMode());
        NodeServiceConfig nodeServiceConfig = new NodeServiceConfig(nodeSocket);
        ClientServiceConfig clientServiceConfig = new ClientServiceConfig(clientSocket);

        Genesis genesis = loadGenesis();

        System.out.println(LOGO);
        System.out.println("f = " + numFaults);
        ServiceContainer container = new ServiceContainer();
        container.addResource(GlobalServiceConfig.class, globalConfig);
        container.addResource(NodeServiceConfig.class, nodeServiceConfig);
        container.addResource(ClientServiceConfig.class, clientServiceConfig);
        container.addResource(Genesis.class, genesis);
        container.register(BlockchainService.class);
        container.register(NodeService.class);
        container.register(ClientService.class);
        container.register(ConsensusService.class);
        container.register(StateService.class);
        container.init();
        container.start();
    }

    private static Genesis loadGenesis() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Account.class, new AccountDeserializer())
                .create();
        try {
            String json = Files.readString(Path.of("genesis.json"));
            return gson.fromJson(json, Genesis.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
