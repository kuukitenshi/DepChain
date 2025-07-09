package pt.ulisboa.tecnico.sec.depchain.node.command_line;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Optional;

import pt.ulisboa.tecnico.sec.depchain.common.info.NodeInfo;
import pt.ulisboa.tecnico.sec.depchain.common.info.SystemInfo;

public record CommandLineArguments(NodeInfo self, SystemInfo systemInfo, KeyStore keyStore,
        ExecutionMode executionMode) {

    public enum ExecutionMode {
        DIFFERENT_WRITE,
        DIFFERENT_ACCEPT,
        DIFFERENT_CLIENT,
        NO_RESPONSE,
        CRASH,
        CORRECT
    }

    public static CommandLineArguments parse(String[] args) throws CommandLineException {
        if (args.length < 3 || args.length > 4) {
            throw new CommandLineException(
                    "Invalid arguments, use: <node id> <members file> <keystore file> [execution mode]");
        }
        String self = args[0];
        File membersFile = new File(args[1]);
        SystemInfo systemInfo;
        try {
            systemInfo = SystemInfo.load(membersFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommandLineException("Couldn't load members file!");
        }
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(new File(args[2]), "secgroup03".toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new CommandLineException("Couldn't load keystore!");
        }
        ExecutionMode execMode = ExecutionMode.CORRECT;
        if (args.length == 4) {
            try {
                execMode = ExecutionMode.valueOf(args[3].toUpperCase());
            } catch (Exception e) {
                throw new CommandLineException("Invalid execution mode!");
            }
        }
        Optional<NodeInfo> optInfo = systemInfo.nodes().stream().filter(node -> node.id().equals(self))
                .findFirst();
        if (optInfo.isEmpty()) {
            throw new CommandLineException("Node id not present in members list!");
        }
        return new CommandLineArguments(optInfo.get(), systemInfo, keyStore, execMode);
    }

}
