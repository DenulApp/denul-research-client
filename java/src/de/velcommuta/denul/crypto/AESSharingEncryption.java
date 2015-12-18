package de.velcommuta.denul.crypto;

import com.google.protobuf.InvalidProtocolBufferException;

import javax.crypto.BadPaddingException;

import de.velcommuta.denul.data.DataBlock;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.data.ShareableUnwrapper;
import de.velcommuta.denul.data.TokenPair;
import de.velcommuta.denul.data.proto.DataContainer;

import java.util.logging.Logger;

/**
 * {@link SharingEncryption} implementation using AES256-GCM
 */
public class AESSharingEncryption implements SharingEncryption {
    private static final Logger logger = Logger.getLogger(AESSharingEncryption.class.getName());


    @Override
    public byte[] encryptKeysAndIdentifier(DataBlock data, KeySet keys) {
        // Assemble plaintext
        // TODO Convert to Protobuf or another more sensible format?
        byte[] plaintext = new byte[data.getIdentifier().length + data.getKey().length];
        System.arraycopy(data.getIdentifier(), 0, plaintext, 0,                           data.getIdentifier().length);
        System.arraycopy(data.getKey(),        0, plaintext, data.getIdentifier().length, data.getKey().length);
        // Encrypt identifier-key-pair and return result
        // The result is bound to the identifier because the identifier is bound to the outbound counter,
        // which is used as IV and thus implicitly authenticated (decryption will fail if it is wrong)
        return AES.encryptAES(plaintext, keys.getOutboundKey(), null, keys.getOutboundCtr());
    }


    @Override
    public Shareable decryptShareable(DataBlock encrypted) {
        // Prepare byte[] for decrypted data
        DataContainer.Wrapper wrapper;
        try {
            // Decrypt
            byte[] decrypted = AES.decryptAES(encrypted.getCiphertext(), encrypted.getKey(), encrypted.getIdentifier());
            wrapper = DataContainer.Wrapper.parseFrom(decrypted);
        } catch (BadPaddingException e) {
            // Decryption failed - probably because of authentication issues
            logger.severe("decryptShareable: BadPaddingException - Authentication failed");
            return null;
        } catch (InvalidProtocolBufferException e) {
            logger.severe("decryptShareable: InvalidProtocolBufferException");
            return null;
        }
        Shareable rv = ShareableUnwrapper.unwrap(wrapper);
        if (rv != null && encrypted.getOwner() != null && encrypted.getOwner().getID() != -1) rv.setOwner(encrypted.getOwner().getID());
        return rv;
    }


    @Override
    public DataBlock decryptKeysAndIdentifier(byte[] encrypted, KeySet keys) {
        byte[] decrypted;
        try {
            decrypted = AES.decryptAES(encrypted, keys.getInboundKey(), null, keys.getInboundCtr());
        } catch (BadPaddingException e) {
            logger.severe("decryptKeysAndIdentifier: BadPaddingException");
            return null;
        }
        if (decrypted == null || decrypted.length != 64) {
            logger.severe("decryptKeysAndIdentifier: Bad decrypted data");
            return null;
        }
        byte[] identifier = new byte[32];
        byte[] key = new byte[32];
        System.arraycopy(decrypted, 0,  identifier, 0, 32);
        System.arraycopy(decrypted, 32, key,        0, 32);
        return new DataBlock(key, identifier);
    }
}
