package de.velcommuta.denul.data;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A DataBlock contains encrypted {@link Shareable} data
 */
public class DataBlock {
    private static final Logger logger = Logger.getLogger(DataBlock.class.getName());


    private byte[] mKey;
    private byte[] mIdentifier;
    private byte[] mCiphertext;
    private Friend mOwner;
    private int mGranularity;


    /**
     * Constructor for data object
     * @param key The key used to encrypt the data
     * @param ciphertext The encrypted data
     * @param identifier The identifier associated with this data
     */
    public DataBlock(byte[] key, byte[] ciphertext, byte[] identifier) {
        mKey = Arrays.copyOf(key, key.length);
        mCiphertext = Arrays.copyOf(ciphertext, ciphertext.length);
        mIdentifier = Arrays.copyOf(identifier, identifier.length);
    }

    /**
     * Constructor for data object
     * @param key The key used to encrypt the data
     * @param ciphertext The encrypted data
     * @param identifier The identifier associated with this data
     * @param granularity The granularity
     */
    public DataBlock(byte[] key, byte[] ciphertext, byte[] identifier, int granularity) {
        mKey = Arrays.copyOf(key, key.length);
        mCiphertext = Arrays.copyOf(ciphertext, ciphertext.length);
        mIdentifier = Arrays.copyOf(identifier, identifier.length);
        mGranularity = granularity;
    }


    /**
     * Constructor for the data object if the ciphertext is not known
     * @param key The key used to encrypt the data
     * @param identifier The identifier of the data
     */
    public DataBlock(byte[] key, byte[] identifier) {
        mKey = Arrays.copyOf(key, key.length);
        mIdentifier = Arrays.copyOf(identifier, identifier.length);
        mCiphertext = null;
    }


    /**
     * Setter for the ciphertext, IF the ciphertext has not yet been set
     * @param ciphertext The ciphertext
     */
    public void setCiphertext(byte[] ciphertext) {
        if (mCiphertext == null) {
            mCiphertext = Arrays.copyOf(ciphertext, ciphertext.length);
        } else {
            logger.severe("setCiphertext: Ciphertext already set");
        }
    }


    /**
     * Getter for the encryption key
     * @return The encryption key
     */
    public byte[] getKey() {
        return Arrays.copyOf(mKey, mKey.length);
    }


    /**
     * Getter for the ciphertext
     * @return The ciphertext
     */
    public byte[] getCiphertext() {
        return mCiphertext;
    }


    /**
     * Getter for the Identifier
     * @return The Identifier
     */
    public byte[] getIdentifier() {
        return mIdentifier;
    }


    /**
     * Setter for the optional Owner field
     * @param owner The owner of the data
     */
    public void setOwner(Friend owner) {
        mOwner = owner;
    }


    /**
     * Set the granularity level of this datablock
     * @param granularity One of the GRANULARITY_* constants defined in the {@link Shareable} interface
     */
    public void setGranularity(int granularity) {
        mGranularity = granularity;
    }


    /**
     * Getter for the granularity
     * @return The granularity level
     */
    public int getGranularity() {
        return mGranularity;
    }


    /**
     * Getter for the optional owner field
     * @return The owner, if set, or null;
     */
    public Friend getOwner() {
        return mOwner;
    }

    public boolean equals(Object o) {
        if (!(o instanceof DataBlock)) return false;
        DataBlock d = (DataBlock) o;
        return Arrays.equals(getCiphertext(), d.getCiphertext())
                && Arrays.equals(getKey(), d.getKey())
                && Arrays.equals(getIdentifier(), d.getIdentifier())
                && getOwner().equals(d.getOwner());
    }
}
