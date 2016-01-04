package de.velcommuta.denul.database;

import de.velcommuta.denul.crypto.ECDHKeyExchange;
import de.velcommuta.denul.crypto.RSA;
import de.velcommuta.denul.data.*;

import java.io.*;
import java.security.KeyPair;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

// Static import contract classes
import static de.velcommuta.denul.database.SQLContract.Studies;
import static de.velcommuta.denul.database.SQLContract.Investigators;
import static de.velcommuta.denul.database.SQLContract.DataRequests;
import static de.velcommuta.denul.database.SQLContract.StudyParticipants;
import static de.velcommuta.denul.database.SQLContract.Data.LocationLog;
import static de.velcommuta.denul.database.SQLContract.Data.LocationSessions;

/**
 * A database backend utilizing SQLite with SQLite-JDBC by Xerial.
 * https://github.com/xerial/sqlite-jdbc
 * (The library is licensed Apache v2)
 */
public class SQLiteDatabase implements Database {
    // Instance variables
    private Connection mConnection;

    /**
     * Public constructor
     */
    public SQLiteDatabase() {
        this("data.db");
    }

    /**
     * Protected constructor, only for unittesting use
     * @param filename The filename of the database
     */
    protected SQLiteDatabase(String filename) {
        assert filename != null && !filename.equals("");
        try {
            mConnection = DriverManager.getConnection("jdbc:sqlite:" + filename);
            Statement stmt = null;
            try {
                // Create a statement object
                stmt = mConnection.createStatement();
                stmt.execute("PRAGMA FOREIGN_KEYS = ON;");
                stmt.execute(Studies.CREATE);
                stmt.execute(Investigators.CREATE);
                stmt.execute(DataRequests.CREATE);
                stmt.execute(StudyParticipants.CREATE);
                stmt.execute(LocationSessions.CREATE);
                stmt.execute(LocationLog.CREATE);
            } catch (SQLException e) {
                // Something went wrong, print stacktrace
                e.printStackTrace();
                throw new IllegalArgumentException("Database error: " + e);
            } finally {
                // Close statement to free up memory
                if (stmt != null) stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Database error: " + e);
        }
    }

    @Override
    public void close() {
        if (isOpen()) try {
            mConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // (name, institution, webpage, description, purpose, procedures, risks, benefits, payment, conflicts, confidentiality, participationAndWithdrawal, rights,
    // verification, privkey, pubkey, keyalgo, kex, kexalgo, queue)
    @Override
    public long addStudyRequest(StudyRequest req) {
        assert isOpen();
        assert req != null;
        long rv;
        Savepoint before = null;
        try {
            // Set up a savepoint to roll back to in case of problems
            before = mConnection.setSavepoint();
            // Insert the StudyRequest itself
            PreparedStatement stmt = mConnection.prepareStatement(Studies.INSERT, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1,  req.name);
            stmt.setString(2,  req.institution);
            stmt.setString(3,  req.webpage);
            stmt.setString(4,  req.description);
            stmt.setString(5,  req.purpose);
            stmt.setString(6,  req.procedures);
            stmt.setString(7,  req.risks);
            stmt.setString(8,  req.benefits);
            stmt.setString(9,  req.payment);
            stmt.setString(10, req.conflicts);
            stmt.setString(11, req.confidentiality);
            stmt.setString(12, req.participationAndWithdrawal);
            stmt.setString(13, req.rights);
            stmt.setInt   (14, req.verification);
            stmt.setString(15, RSA.encodeKey(req.privkey));
            stmt.setString(16, RSA.encodeKey(req.pubkey));
            stmt.setInt   (17, 1);  // TODO Change constants
            stmt.setBytes (18, serializeKeyPair(req.exchange.getKeypair()));
            stmt.setInt   (19, 1);  // TODO Change constants
            stmt.setBytes (20, req.queue);
            int affected_rows = stmt.executeUpdate();
            // Determine the ID of the inserted column
            // Inserted object ID identification loosely based on http://stackoverflow.com/a/1915197/1232833
            assert affected_rows > 0;
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                rv = generatedKeys.getLong(1);
            } else {
                mConnection.rollback(before);
                throw new IllegalArgumentException("Insert failed, no record created");
            }
            stmt.close();
            // Set ID on object
            req.id = rv;
            // Insert Investigators
            stmt = mConnection.prepareStatement(Investigators.INSERT);
            for (StudyRequest.Investigator inv : req.investigators) {
                stmt.setLong(1, rv);
                stmt.setString(2, inv.name);
                stmt.setString(3, inv.institution);
                stmt.setString(4, inv.group);
                stmt.setString(5, inv.position);
                affected_rows = stmt.executeUpdate();
                assert affected_rows > 0;
            }
            stmt.close();
            // Insert DataRequests
            stmt = mConnection.prepareStatement(DataRequests.INSERT);
            for (StudyRequest.DataRequest data : req.requests) {
                stmt.setLong(1, rv);
                stmt.setInt(2, data.type);
                stmt.setInt(3, data.granularity);
                stmt.setInt(4, data.frequency);
                affected_rows = stmt.executeUpdate();
                assert affected_rows > 0;
            }
            stmt.close();
            // Commit
            mConnection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (before != null) try {
                mConnection.rollback(before);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            throw new IllegalArgumentException("SQL Exception: ", e);
        }
        return rv;
    }

    @Override
    public StudyRequest getStudyRequestByID(long id) {
        assert isOpen();
        assert id >= 0;
        StudyRequest rv;
        try {
            // Prepare query
            PreparedStatement stmt = mConnection.prepareStatement(Studies.SELECT_BY_ID);
            stmt.setLong(1, id);
            // Execute query
            ResultSet rs = stmt.executeQuery();
            // We expect only one result
            if (!rs.next()) {
                // No result => ID is not in the database
                return null;
            }
            rv = studyRequestFromResultSet(rs);
            stmt.close();
            rs.close();

            // Retrieve Investigators
            rv.investigators.addAll(getInvestigatorsForStudyID(id));

            // Retrieve DataRequests
            rv.requests.addAll(getDataRequesstsForStudyID(id));
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("SQL Error: ", e);
        }
        return rv;
    }

    @Override
    public long getStudyIDByQueueIdentifier(byte[] identifier) {
        assert isOpen();
        assert identifier != null;
        long rv = -1;
        try {
            // Prepare statement
            PreparedStatement stmt = mConnection.prepareStatement(Studies.SELECT_BY_QUEUE);
            // Set parameters
            stmt.setBytes(1, identifier);
            // Perform query
            ResultSet rs = stmt.executeQuery();
            // We expect only one result, as Queue Identifiers should be unique
            if (rs.next()) {
                rv = rs.getLong(1);
            }
            // Close resultset
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("SQL Exception: ", e);
        }
        return rv;
    }

    @Override
    public List<StudyRequest> getStudyRequests() {
        assert isOpen();
        List<StudyRequest> rv = new LinkedList<>();
        try {
            PreparedStatement stmt = mConnection.prepareStatement(Studies.SELECT_ALL);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                StudyRequest req = studyRequestFromResultSet(rs);
                req.investigators.addAll(getInvestigatorsForStudyID(req.id));
                req.requests.addAll(getDataRequesstsForStudyID(req.id));
                rv.add(req);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("SQL Error: ", e);
        }
        return rv;
    }

    @Override
    public long addParticipant(KeySet keys, long studyid) {
        assert isOpen();
        assert keys != null;
        assert studyid >= 0;
        long rv = -1;
        try {
            // Prepare insert
            PreparedStatement stmt = mConnection.prepareStatement(StudyParticipants.INSERT);
            // Set parameters
            stmt.setLong(1, studyid);
            stmt.setBytes(2, keys.getOutboundKey());
            stmt.setBytes(3, keys.getOutboundCtr());
            stmt.setBytes(4, keys.getInboundKey());
            stmt.setBytes(5, keys.getInboundCtr());
            // Execute update
            int affected_rows = stmt.executeUpdate();
            // Ensure update worked
            assert affected_rows > 0;
            // Retrieve ID of inserted row
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                rv = generatedKeys.getLong(1);
            } else {
                throw new IllegalArgumentException("Insert failed, no record created");
            }
            // Commit changes to DB
            mConnection.commit();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("SQL Error: ", e);
        }
        return rv;
    }

    @Override
    public List<KeySet> getParticipants() {
        assert isOpen();
        List<KeySet> rv = new LinkedList<>();
        try {
            PreparedStatement stmt = mConnection.prepareStatement(StudyParticipants.SELECT_ALL);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                KeySet ks = keySetFromResultSet(rs);
                rv.add(ks);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("SQL Exception: ", e);
        }
        return rv;
    }

    @Override
    public long getParticipantIDByKeySet(KeySet keys) {
        assert isOpen();
        assert keys != null;
        long rv = -1;
        try {
            PreparedStatement stmt = mConnection.prepareStatement(StudyParticipants.SELECT_KEYS);
            stmt.setBytes(1, keys.getOutboundKey());
            stmt.setBytes(2, keys.getOutboundCtr());
            stmt.setBytes(3, keys.getInboundKey());
            stmt.setBytes(4, keys.getInboundCtr());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                rv = rs.getLong(1);
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("SQL Exception: ", e);
        }
        return rv;
    }

    @Override
    public void addGPSTrack(GPSTrack track, long ownerid) {
        assert isOpen();
        assert track != null;
        assert ownerid >= 0;
        try {
            long rv;
            // Prepare insert
            PreparedStatement stmt = mConnection.prepareStatement(LocationSessions.INSERT);
            // Set parameters
            stmt.setString(1, track.getSessionName());
            stmt.setLong(2, ownerid);
            stmt.setLong(3, track.getTimestamp());
            stmt.setLong(4, track.getTimestampEnd());
            stmt.setString(5, track.getTimezone());
            stmt.setFloat(6, track.getDistance());
            stmt.setInt(7, track.getModeOfTransportation());
            stmt.setString(8, track.getDescription());
            // Execute
            int changed = stmt.executeUpdate();
            assert changed > 0;
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                rv = generatedKeys.getLong(1);
            } else {
                throw new IllegalArgumentException("Insert failed, no record created");
            }
            stmt.close();
            PreparedStatement innerstmt = mConnection.prepareStatement(LocationLog.INSERT);
            for (Location loc : track.getPosition()) {
                innerstmt.setLong(1, rv);
                innerstmt.setDouble(2, loc.getTime());
                innerstmt.setDouble(3, loc.getLatitude());
                innerstmt.setDouble(4, loc.getLongitude());
                changed = innerstmt.executeUpdate();
                assert changed > 0;
            }
            innerstmt.close();
            mConnection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("SQL Error: ", e);
        }
    }

    @Override
    public List<GPSTrack> getGPSTracks() {
        assert isOpen();
        List<GPSTrack> rv = new LinkedList<>();
        try {
            PreparedStatement stmt = mConnection.prepareStatement(LocationSessions.SELECT_ALL);
            PreparedStatement innerstmt = mConnection.prepareStatement(LocationLog.SELECT_ID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                innerstmt.setLong(1, id);
                ResultSet irs = innerstmt.executeQuery();
                List<Location> loclist = new LinkedList<>();
                while (irs.next()) {
                    Location loc = new Location();
                    loc.setTime(irs.getDouble(3));
                    loc.setLatitude(irs.getDouble(4));
                    loc.setLongitude(irs.getDouble(5));
                    loclist.add(loc);
                }
                GPSTrack track = new GPSTrack(loclist, // locations
                        rs.getString(2), // Name
                        rs.getInt(8), // Mode
                        rs.getLong(4),  // Timestamp start
                        rs.getLong(5),  // Timestamp end
                        rs.getString(6),  // Timezone
                        rs.getFloat(7)); // Distance
                track.setDescription(rs.getString(9)); // Description
                track.setID((int) rs.getLong(1)); // ID
                irs.close();
                rv.add(track);
            }
            rs.close();
            stmt.close();
            innerstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("SQL Error: ", e);
        }
        return rv;
    }

    @Override
    public List<Shareable> getDataByParticipantID(long participantID) {
        // TODO
        return null;
    }

    @Override
    public List<Shareable> getDataByStudyID(long studyid) {
        // TODO
        return null;
    }

    /**
     * Check if the underlying database is open
     * @return true if the database is open, false otherwise
     */
    private boolean isOpen() {
        try {
            return mConnection != null && !mConnection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    ///// Helper function

    /**
     * Retrieve all {@link de.velcommuta.denul.data.StudyRequest.Investigator}s for a specific study ID
     * @param id The Study ID
     * @return A List of {@link de.velcommuta.denul.data.StudyRequest.Investigator}s, or an empty List if no Investigators
     * are saved in the database
     */
    private List<StudyRequest.Investigator> getInvestigatorsForStudyID(long id) {
        List<StudyRequest.Investigator> rv = new LinkedList<>();
        try {
            PreparedStatement stmt = mConnection.prepareStatement(Investigators.SELECT_STUDY_ID);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rv.add(investigatorFromResultSet(rs));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("SQL Error: ", e);
        }
        return rv;
    }


    /**
     * Retrieve all {@link de.velcommuta.denul.data.StudyRequest.DataRequest}s for a specific study ID
     * @param id The Study ID
     * @return A List of DataRequests, or an empty List if no such requests are saved
     */
    private List<StudyRequest.DataRequest> getDataRequesstsForStudyID(long id) {
        List<StudyRequest.DataRequest> rv = new LinkedList<>();
        try {
            PreparedStatement stmt = mConnection.prepareStatement(DataRequests.SELECT_STUDY_ID);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rv.add(dataRequestFromResultSet(rs));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("SQL Error: ", e);
        }
        return rv;
    }


    /**
     * Read a {@link StudyRequest} from a {@link ResultSet} and return it. Will NOT have the investigators and requests
     * fields set. Will not modify the ResultSet
     * @param rs The ResultSet
     * @return A StudyRequest
     */
    private StudyRequest studyRequestFromResultSet(ResultSet rs) {
        assert rs != null;
        // Create object
        StudyRequest rv = new StudyRequest();
        try {
            rv.id = rs.getLong(Studies.COLUMN_ID);
            rv.name = rs.getString(Studies.COLUMN_NAME);
            rv.institution = rs.getString(Studies.COLUMN_INSTITUTION);
            rv.webpage = rs.getString(Studies.COLUMN_WEB);
            rv.description = rs.getString(Studies.COLUMN_DESCRIPTION);
            rv.purpose = rs.getString(Studies.COLUMN_PURPOSE);
            rv.procedures = rs.getString(Studies.COLUMN_PROCEDURES);
            rv.risks = rs.getString(Studies.COLUMN_RISKS);
            rv.benefits = rs.getString(Studies.COLUMN_BENEFITS);
            rv.payment = rs.getString(Studies.COLUMN_PAYMENT);
            rv.conflicts = rs.getString(Studies.COLUMN_CONFLICTS);
            rv.confidentiality = rs.getString(Studies.COLUMN_CONFIDENTIALITY);
            rv.participationAndWithdrawal = rs.getString(Studies.COLUMN_PARTICIPATION);
            rv.rights = rs.getString(Studies.COLUMN_RIGHTS);
            rv.verification = rs.getInt(Studies.COLUMN_VERIFICATION);
            rv.pubkey = RSA.decodePublicKey(rs.getString(Studies.COLUMN_PUBKEY));
            rv.privkey = RSA.decodePrivateKey(rs.getString(Studies.COLUMN_PRIVKEY));
            rv.exchange = new ECDHKeyExchange(deserializeKeyPair(rs.getBytes(Studies.COLUMN_KEX)));
            rv.queue = rs.getBytes(Studies.COLUMN_QUEUE);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("ResultSet seems bad: ", e);
        }
        return rv;
    }

    /**
     * Read a KeySet from a ResultSet
     * @param rs The ResultSet
     * @return The KeySet
     * @throws SQLException If the ResultSet throws it
     */
    private KeySet keySetFromResultSet(ResultSet rs) throws SQLException {
        assert rs != null;
        return new KeySet( // KeyIn, KeyOut, CtrIn, CtrOut, initiated, databaseID
                rs.getBytes(5),
                rs.getBytes(3),
                rs.getBytes(6),
                rs.getBytes(4),
                true,
                rs.getInt(1)
        );
    }

    /**
     * Read a {@link de.velcommuta.denul.data.StudyRequest.Investigator} from a {@link ResultSet} and return it.
     * Will not modify the ResultSet
     * @param rs The ResultSet
     * @return An {@link de.velcommuta.denul.data.StudyRequest.Investigator}
     */
    private StudyRequest.Investigator investigatorFromResultSet(ResultSet rs) {
        // Initialize reply object
        StudyRequest.Investigator rv = new StudyRequest.Investigator();
        try {
            // Set fields
            rv.name = rs.getString(Investigators.COLUMN_NAME);
            rv.institution = rs.getString(Investigators.COLUMN_INSTITUTION);
            rv.group = rs.getString(Investigators.COLUMN_GROUP);
            rv.position = rs.getString(Investigators.COLUMN_POSITION);
        } catch (SQLException e) {
            // Something is very wrong
            e.printStackTrace();
            throw new IllegalArgumentException("ResultSet seems bad: ", e);
        }
        return rv;
    }


    /**
     * Read a {@link de.velcommuta.denul.data.StudyRequest.DataRequest} from a {@link ResultSet} and return it.
     * Will not modify the ResultSet
     * @param rs The ResultSet
     * @return A {@link de.velcommuta.denul.data.StudyRequest.DataRequest}
     */
    private StudyRequest.DataRequest dataRequestFromResultSet(ResultSet rs) {
        // Initialize reply object
        StudyRequest.DataRequest rv = new StudyRequest.DataRequest();
        try {
            // Set fields
            rv.type = rs.getInt(DataRequests.COLUMN_DATATYPE);
            rv.granularity = rs.getInt(DataRequests.COLUMN_GRANULARITY);
            rv.frequency = rs.getInt(DataRequests.COLUMN_FREQUENCY);
        } catch (SQLException e) {
            // Something is very wrong
            e.printStackTrace();
            throw new IllegalArgumentException("ResultSet seems bad: ", e);
        }
        return rv;
    }

    /**
     * Serialize a Keypair
     * @param serialize The keypair to serialize
     * @return The serialized keypair
     */
    private byte[] serializeKeyPair(KeyPair serialize) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(serialize);

            byte[] res = b.toByteArray();
            o.close();
            b.close();
            return res;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[] {0x00};
        }
    }

    /**
     * Deserialize a serialized keypair
     * @param serialized The serialized keypair
     * @return The deserialized keypair
     */
    private KeyPair deserializeKeyPair(byte[] serialized) {
        try {
            ByteArrayInputStream bi = new ByteArrayInputStream(serialized);
            ObjectInputStream oi = new ObjectInputStream(bi);
            Object obj = oi.readObject();
            oi.close();
            bi.close();

            assert obj instanceof KeyPair;
            return (KeyPair) obj;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
