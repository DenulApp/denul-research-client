package de.velcommuta.denul.crypto;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECParameterSpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

import javax.crypto.KeyAgreement;

/**
 * Key exchange using ECDH with Curve25519.
 * Code partially based on these StackExchange posts:
 * Curve25519 parameters: http://stackoverflow.com/a/30014831/1232833
 * Key Exchange logic: http://stackoverflow.com/q/18285073/1232833
 * byte[]-to-PublicKey parsing: http://stackoverflow.com/a/4969415/1232833
 */
public class ECDHKeyExchange implements KeyExchange {

    private static final Logger logger = Logger.getLogger(ECDHKeyExchange.class.getName());


    // KeyAgreement instance
    private KeyAgreement mKeyAgree;
    private KeyPair mKeypair;
    private boolean mPhaseSuccess = false;

    // Insert BouncyCastle provider
    static {
        Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    /**
     * Public constructor. Initialize everything to prepare for a Curve25519 key exchange.
     * Curve25519 initialization based on http://stackoverflow.com/a/30014831/1232833
     */
    public ECDHKeyExchange() {
        // Get Curve25519 in X9.62 form
        X9ECParameters ecP = CustomNamedCurves.getByName("curve25519");
        // convert to JCE form
        ECParameterSpec ecSpec = new ECParameterSpec(ecP.getCurve(), ecP.getG(),
                ecP.getN(), ecP.getH(), ecP.getSeed());
        try {
            // Get Keypair generator based on the spec
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDH", "BC");
            keyGen.initialize(ecSpec, new SecureRandom());
            // Generate a keypair
            mKeypair = keyGen.generateKeyPair();

            // Get a keyAgreement instance
            mKeyAgree = KeyAgreement.getInstance("ECDH", "BC");
            // Initialize the KeyAgreement
            mKeyAgree.init(mKeypair.getPrivate());
        } catch (NoSuchAlgorithmException e) {
            // We need to catch these exception, but they should never occur, as we are bundling
            // a Spongycastle version that includes these algorithms
            logger.severe("Constructor: NoSuchAlgorithm: " + e.getMessage());
        } catch (NoSuchProviderException e) {
            logger.severe("Constructor: NoSuchProvider: " + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            logger.severe("Constructor: InvalidAlgorithmParameterException: " + e.getMessage());
        } catch (InvalidKeyException e) {
            logger.severe("Constructor: InvalidKeyException: " + e.getMessage());
        }
    }


    @Override
    public byte[] getPublicKexData() {
        if (mKeypair != null) {
            return mKeypair.getPublic().getEncoded();
        } else {
            logger.severe("getPublicKexData: mKeypair == null");
            return null;
        }
    }


    @Override
    public boolean putPartnerKexData(byte[] data) {
        if (mPhaseSuccess) {
            logger.severe("putPartnerKexData: Already received kex data, ignoring");
            return false;
        }
        // Code based on http://stackoverflow.com/a/4969415/1232833
        // Import data into KeySpec
        X509EncodedKeySpec ks = new X509EncodedKeySpec(data);
        // Prepare a keyFactory
        KeyFactory kfac;
        try {
            // initialize key factory
            kfac = KeyFactory.getInstance("ECDH", "BC");
        } catch (NoSuchAlgorithmException e) {
            logger.severe("putPartnerKexData: NoSuchAlgorithm: " + e.getMessage());
            return false;
        } catch (NoSuchProviderException e) {
            logger.severe("putPartnerKexData: NoSuchProvider: " + e.getMessage());
            return false;
        }

        // Prepare public key variable
        ECPublicKey remotePubkey;
        try {
            // Parse the public key
            remotePubkey = (ECPublicKey) kfac.generatePublic(ks);
        } catch (InvalidKeySpecException e) {
            logger.severe("putPartnerKexData: Invalid data received!" + e.getMessage());
            return false;
        } catch (ClassCastException e) {
            logger.severe("putPartnerKexData: Key data was valid, but no ECPublicKey. " + e.getMessage());
            return false;
        }

        try {
            mKeyAgree.doPhase(remotePubkey, true);
        } catch (InvalidKeyException e) {
            logger.severe("putPartnerKexData: Invalid key: " + e.getMessage());
            return false;
        }
        mPhaseSuccess = true;
        return true;
    }


    @Override
    public byte[] getAgreedKey() {
        if (mPhaseSuccess) {
            return mKeyAgree.generateSecret();
        } else {
            logger.severe("getAgreedKey: Key agreement has not concluded successfully!");
            return null;
        }
    }
}
