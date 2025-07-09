package pt.ulisboa.tecnico.sec.depchain.common.utils;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;

public class CryptoUtils {

    private static final SecureRandom RNG = new SecureRandom();
    private static final String MAC_ALGORITHM = "HmacSHA256";
    private static final String SECREY_KEY_ALGORITHM = "AES";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String AGREEMENT_ALGORITHM = "DH";

    public static byte[] mac(Key key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(key);
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean verifyMac(Key key, byte[] data, byte[] mac) {
        return MessageDigest.isEqual(mac(key, data), mac);
    }

    public static byte[] sign(PrivateKey key, Object... objects) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(key);
            byte[] data = ByteUtils.serialize(objects);
            signature.update(data);
            return signature.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            return null;
        }
    }

    public static boolean verifySignature(PublicKey key, byte[] signature, Object... objects) {
        try {
            Signature engine = Signature.getInstance(SIGNATURE_ALGORITHM);
            engine.initVerify(key);
            byte[] data = ByteUtils.serialize(objects);
            engine.update(data);
            return engine.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            return false;
        }
    }

    public static KeyPair genDiffieKeyPair() {
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance(AGREEMENT_ALGORITHM);
            generator.initialize(2048);
            return generator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Key genSharedSecret(PrivateKey prk, PublicKey puk) {
        KeyAgreement agreement;
        try {
            agreement = KeyAgreement.getInstance(AGREEMENT_ALGORITHM);
            agreement.init(prk);
            agreement.doPhase(puk, true);
            byte[] secret = agreement.generateSecret();
            return new SecretKeySpec(secret, 0, 32, SECREY_KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalStateException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] hash(Object... data) {
        byte[] bytes = ByteUtils.serialize(data);
        return Hash.sha3(bytes);
    }

    public static long genNonce() {
        return RNG.nextLong();
    }

    public static ClientSignatureData ecSign(ECKeyPair keyPair, Object... data) {
        byte[] hash = CryptoUtils.hash(data);
        SignatureData signature = Sign.signMessage(hash, keyPair);
        return new ClientSignatureData(signature);
    }

    public static BigInteger getSignaturePubKey(SignatureData signature, Object... data) throws SignatureException {
        byte[] hash = CryptoUtils.hash(data);
        return Sign.signedMessageToKey(hash, signature);
    }

    public static ECKeyPair createECKeyPair() {
        try {
            return Keys.createEcKeyPair();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            return null;
        }
    }

}
