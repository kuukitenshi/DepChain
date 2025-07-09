package pt.ulisboa.tecnico.sec.depchain.node;

import pt.ulisboa.tecnico.sec.depchain.common.net.AuthenticatedPerfectLink;

public record BlockchainProcess(String id, AuthenticatedPerfectLink link) {
}
