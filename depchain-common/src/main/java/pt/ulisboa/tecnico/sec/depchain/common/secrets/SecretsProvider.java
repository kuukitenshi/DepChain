package pt.ulisboa.tecnico.sec.depchain.common.secrets;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface SecretsProvider {

    PrivateKey getPrivateKey(String alias);

    PublicKey getPublicKey(String alias);

}
