package de.velcommuta.denul.crypto;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.security.KeyPair;

/**
 * A stub Key Exchange that is only used to store public kex data
 */
public class KexStub implements KeyExchange {
    private byte[] mKexData;

    /**
     * Constructor
     * @param pubdata Public key data
     */
    public KexStub(byte[] pubdata) {
        mKexData = pubdata;
    }

    @Override
    public byte[] getPublicKexData() {
        return mKexData;
    }

    // Key Exchange functions do not work on this
    @Override
    public boolean putPartnerKexData(byte[] data) {
        throw new NotImplementedException();
    }

    @Override
    public byte[] getAgreedKey() {
        throw new NotImplementedException();
    }

    @Override
    public KeyPair getKeypair() {
        throw new NotImplementedException();
    }

    @Override
    public void reset() {throw new NotImplementedException();}
}
