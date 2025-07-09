package pt.ulisboa.tecnico.sec.depchain.common.secrets;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateException;

public class KeystoreSecretsProvider implements SecretsProvider {

    private final KeyStore keyStore;
    private final char[] password;

    public KeystoreSecretsProvider(File keyStoreFile, char[] password)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        this(KeyStore.getInstance(keyStoreFile, password), password);
    }

    public KeystoreSecretsProvider(KeyStore keyStore, char[] password) {
        this.keyStore = keyStore;
        this.password = password;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        try {
            PrivateKeyEntry entry = (PrivateKeyEntry) this.keyStore.getEntry(alias,
                    new KeyStore.PasswordProtection(this.password));
            return entry.getPrivateKey();
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
            return null;
        }
    }

    @Override
    public PublicKey getPublicKey(String alias) {
        try {
            PrivateKeyEntry entry = (PrivateKeyEntry) this.keyStore.getEntry(alias,
                    new KeyStore.PasswordProtection(this.password));
            return entry.getCertificate().getPublicKey();
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
            return null;
        }
    }

}
