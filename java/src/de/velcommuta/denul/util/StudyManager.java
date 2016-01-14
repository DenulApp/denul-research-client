package de.velcommuta.denul.util;

import de.velcommuta.denul.crypto.AESSharingEncryption;
import de.velcommuta.denul.crypto.IdentifierDerivation;
import de.velcommuta.denul.crypto.SHA256IdentifierDerivation;
import de.velcommuta.denul.crypto.SharingEncryption;
import de.velcommuta.denul.data.*;
import de.velcommuta.denul.database.Database;
import de.velcommuta.denul.networking.Connection;
import de.velcommuta.denul.networking.ProtobufProtocol;
import de.velcommuta.denul.networking.Protocol;
import de.velcommuta.denul.networking.TLSConnection;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class providing static functions to perform study management. All-in-one solution for network- and database side of
 * study management, data retrieval, and so on
 */
public class StudyManager {
    private static final Logger logger = Logger.getLogger(ProtobufProtocol.class.getName());

    /**
     * Register a Study on the server and add it to the local database
     * @param req The study request
     * @param db The database handler
     * @return True if the study registration was successful, false otherwise
     */
    public static boolean registerStudy(StudyRequest req, Database db) {
        try {
            // Establish connection to the server
            Connection c = new TLSConnection(Config.getServerHost(), Config.getServerPort());
            Protocol p = new ProtobufProtocol();
            p.connect(c);

            // Register study
            if (p.registerStudy(req) == Protocol.CONNECT_OK) {
                // Registration okay, save to database
                db.addStudyRequest(req);
            } else {
                // TODO Not very helpful
                throw new IllegalArgumentException("Upload failed");
            }

            // Disconnect from the server
            p.disconnect();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Delete a study, both locally and on the server
     * @param req The studyrequest
     * @param db The database to use
     * @return True if the deletion was successful, false otherwise
     */
    public static boolean deleteStudy(StudyRequest req, Database db) {
        try {
            Connection c = new TLSConnection(Config.getServerHost(), Config.getServerPort());
            Protocol p = new ProtobufProtocol();
            p.connect(c);

            int rv = p.deleteStudy(req);
            if (rv == Protocol.SDEL_FAIL_NO_CONNECTION) {
                logger.severe("deleteStudy: FAIL NO CONNECTION");
                return false;
            }
            db.deleteStudy(req);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Retrieve a List of the StudyRequests registered by this client
     * @param db The database to use
     * @return A List of StudyRequests, or an empty List if none have been registered, or null if a database error
     * occured
     */
    public static List<StudyRequest> getMyStudies(Database db) {
        return db.getStudyRequests();
    }


    /**
     * Retrieve the list of study participants for a particular study
     * @param req The studyrequest
     * @param db The database to use
     * @return The list of study participants
     */
    public static List<KeySet> getStudyParticipants(StudyRequest req, Database db) {
        return db.getParticipantsForStudy(db.getStudyIDByQueueIdentifier(req.queue));
    }


    /**
     * Retrieve new data for all registered studies
     * @param db The database to use
     */
    public static void updateAllStudyData(Database db) {
        try {
            Connection c = new TLSConnection(Config.getServerHost(), Config.getServerPort());
            Protocol p = new ProtobufProtocol();
            p.connect(c);
            for (StudyRequest req : getMyStudies(db)) {
                updateStudyData(db, req, p);
            }
            p.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Network error");
        }
    }


    /**
     * Retrieve new data for a specific study
     * @param db The database to use
     * @param req The study
     */
    public static void updateStudyData(Database db, StudyRequest req) {
        try {
            Connection c = new TLSConnection(Config.getServerHost(), Config.getServerPort());
            Protocol p = new ProtobufProtocol();
            p.connect(c);
            updateStudyData(db, req, p);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Network error");
        }
    }


    /**
     * Helper function to perform the actual study update
     * @param db The database to use
     * @param req The study to update
     * @param p The connected {@link Protocol} instance to use
     * @throws IOException If the Protocol throws it
     */
    private static void updateStudyData(Database db, StudyRequest req, Protocol p) throws IOException {
        // Retrieve StudyID from database
        long studyid = db.getStudyIDByQueueIdentifier(req.queue);
        assert studyid >= 0;
        // Look for new registrations for the study
        for (StudyJoinRequest studyjoin: p.getStudyJoinRequests(req)) {
            // Derive keys
            KeySet partner = req.performKex(studyjoin);
            // Add to database
            db.addParticipant(partner, studyid);
        }
        // Retrieve data for all study participants
        retrieve(db, p, db.getParticipantsForStudy(studyid));

    }

    /**
     * Recursively retrieve all available data for a List of KeySets (i.e. study participants)
     * @param db The database to use
     * @param p The protocol to use
     * @param participants The keysets to query
     */
    private static void retrieve(Database db, Protocol p, List<KeySet> participants) {
        // List of TokenPairs to query
        List<TokenPair> query = new LinkedList<>();
        // Map from TokenPairs to associated KeySets
        Map<TokenPair, KeySet> buffer = new HashMap<>();
        // IdentifierDerivation and SharingEncryption instance
        IdentifierDerivation deriv = new SHA256IdentifierDerivation();
        SharingEncryption enc = new AESSharingEncryption();
        // Derive identifiers for all participants
        for (KeySet ks : participants) {
            // derive identifier tokens
            TokenPair tokens = deriv.generateInboundIdentifier(ks);
            // Add to query list and map
            query.add(tokens);
            buffer.put(tokens, ks);
        }
        // Abort if no queries need to be sent
        if (query.size() == 0) return;
        // Retrieve data
        Map<TokenPair, byte[]> result = p.getMany(query);
        // Prepare List of tokens to revoke
        List<TokenPair> revoke = new LinkedList<>();
        // Prepare List of tokens to retrieve
        List<TokenPair> retrieve = new LinkedList<>();
        // List of KeySets that need to be queried again, because they had results
        List<KeySet> requery = new LinkedList<>();
        // Prepare Map from TokenPairs to DataBlocks
        Map<TokenPair, DataBlock> blocks = new HashMap<>();
        // Iterate through results
        for (TokenPair pair : result.keySet()) {
            byte[] value = result.get(pair);
            if (value == Protocol.GET_FAIL_KEY_FMT || value == Protocol.GET_FAIL_NO_CONNECTION || value == Protocol.GET_FAIL_PROTOCOL_ERROR) {
                // Protocol error, ignore
                logger.severe("retrieve: GET of key block FAILED - No connection or other error");
                continue;
            } else if (value == Protocol.GET_FAIL_KEY_NOT_TAKEN) {
                // No value under this key, ignore
                continue;
            }
            // If this statement is reached, value is a key block
            // Retrieve matching KeySet
            KeySet ks = buffer.get(pair);

            if (Arrays.equals(value, new byte[] {0x42})) {
                // Encountered revocation, do nothing
                continue;
            }
            // Decrypt to DataBlock
            DataBlock data = enc.decryptKeysAndIdentifier(value, ks);
            if (data == null) {
                // Decryption failed, ignore - false positive or other weird stuff going on
                logger.severe("retrieve: Decryption of key block FAILED");
                // Increment counters
                ks = deriv.notifyInboundIdentifierUsed(ks);
                // Write changes to database
                db.updateParticipant(ks);
                continue;
            }
            // Increment counters
            ks = deriv.notifyInboundIdentifierUsed(ks);
            // Write changes to database
            db.updateParticipant(ks);
            data.setOwner(ks);
            // Decryption was successful
            // Add to revocation list to remove it from server
            revoke.add(pair);
            // Prepare querying
            TokenPair data_pair = new TokenPair(data.getIdentifier(), data.getIdentifier());
            retrieve.add(data_pair);
            // Add to blocks map so we can later retrieve the encryption key from the data block
            blocks.put(data_pair, data);
        }
        // If we have to perform any revocations, do so now
        if (revoke.size() > 0) p.delMany(revoke);
        if (retrieve.size() == 0) return;
        // We appearently have some more queries to perform
        result = p.getMany(retrieve);
        for (TokenPair ident : result.keySet()) {
            byte[] value = result.get(ident);
            if (value == Protocol.GET_FAIL_KEY_FMT || value == Protocol.GET_FAIL_NO_CONNECTION || value == Protocol.GET_FAIL_PROTOCOL_ERROR) {
                // Protocol error, ignore
                logger.severe("retrieve: Retrieval of data block FAILED - No connection or other weird error");
                continue;
            } else if (value == Protocol.GET_FAIL_KEY_NOT_TAKEN) {
                // No value under this key, ignore
                logger.severe("retrieve: Retrieval of data block FAILED - Key not taken");
                continue;
            }
            // We seem to have retrieved a data block
            DataBlock block = blocks.get(ident);
            block.setCiphertext(value);
            Shareable sh = enc.decryptShareable(block);
            if (sh != null) {
                // Decryption successful, write to Databases
                db.addShareable(sh);
            } else {
                logger.severe("retrieve: Decryption of data block FAILED");
            }
            requery.add(block.getOwner());
        }
        // If any KeySets need to be queried again, do so now
        if (requery.size() != 0) {
            retrieve(db, p, requery);
        }
    }
}
