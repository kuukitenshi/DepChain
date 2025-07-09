package pt.ulisboa.tecnico.sec.depchain.node.services.configs;

import java.net.SocketAddress;
import java.util.Map;

import pt.ulisboa.tecnico.sec.depchain.common.secrets.SecretsProvider;
import pt.ulisboa.tecnico.sec.depchain.node.BlockchainProcess;
import pt.ulisboa.tecnico.sec.depchain.node.command_line.CommandLineArguments.ExecutionMode;

public record GlobalServiceConfig(Map<SocketAddress, BlockchainProcess> processes, BlockchainProcess self,
        SecretsProvider secrets, int numFaults, ExecutionMode execMode) {

}
