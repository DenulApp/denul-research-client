package de.velcommuta.denul.data;

/**
 * Interface implemented by all sharable objects (e.g. run tracks, heart rates, ...).
 * Each implementing class SHOULD also have a static fromByteRepresentation function taking a byte[]
 * and returning a {@link Shareable} object that contains the data from the byte[] representation.
 */
public interface Shareable {
    int SHAREABLE_TRACK     = 0;
    int SHAREABLE_STEPCOUNT = 1;

    int GRANULARITY_FINE        = 0;
    int GRANULARITY_COARSE      = 1;
    int GRANULARITY_VERY_COARSE = 2;

    /**
     * Function to indicate which type the implementing class is. One of the SHAREABLE_* constants
     * defined in the {@link Shareable} interface
     * @return One of the SHAREABLE_* constants indicating which type the object is of
     */
    int getType();

    /**
     * Getter for the ID of the owner. Will be set to the database ID of a {@link Friend}, or -1 if
     * it is owned by the user
     * @return The Owner ID, or -1
     */
    int getOwner();

    /**
     * Setter for the ID of the owner.
     * @param owner The owner ID
     */
    void setOwner(int owner);

    /**
     * Getter for the database ID.
     * @return The database ID of the database entry represented by this sharable, or -1 if no such
     *         database entry exists
     */
    int getID();

    /**
     * Set a description for the shareable. The description will be displayed in the social stream
     * @param description The description, or null to delete an existing description
     */
    void setDescription(String description);

    /**
     * Retrieve the saved description
     * @return The description as a String, or null if it is not set
     */
    String getDescription();
}
