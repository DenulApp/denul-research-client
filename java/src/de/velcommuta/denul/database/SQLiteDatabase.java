package de.velcommuta.denul.database;

import de.velcommuta.denul.crypto.RSA;
import de.velcommuta.denul.data.StudyRequest;

import java.io.*;
import java.security.KeyPair;
import java.sql.*;
import java.util.List;

/**
 * A database backend utilizing SQLite with SQLite-JDBC by Xerial.
 * https://github.com/xerial/sqlite-jdbc
 * (The library is licensed Apache v2)
 */
public class SQLiteDatabase implements Database {
    // Constants
    private static final String CREATE_STUDIES = "CREATE TABLE IF NOT EXISTS Studies (" +
            "id INTEGER PRIMARY KEY, " +
            "name TEXT, " +
            "institution TEXT, " +
            "webpage TEXT, " +
            "description TEXT, " +
            "purpose TEXT, " +
            "procedures TEXT, " +
            "risks TEXT, " +
            "benefits TEXT, " +
            "payment TEXT, " +
            "conflicts TEXT, " +
            "confidentiality TEXT, " +
            "participationAndWithdrawal TEXT, " +
            "rights TEXT, " +
            "verification INT, " +
            "privkey STRING, " +
            "pubkey STRING, " +
            "keyalgo INTEGER, " +
            "kex BLOB, " +
            "kexalgo INTEGER, " +
            "queue BLOB" +
            ");";
    private static final String INSERT_STUDY = "INSERT INTO Studies (name, institution, webpage, description, purpose, "+
            "procedures, risks, benefits, payment, conflicts, confidentiality, participationAndWithdrawal, rights, " +
            "verification, privkey, pubkey, keyalgo, kex, kexalgo, queue) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String CREATE_INVESTIGATORS = "CREATE TABLE IF NOT EXISTS Investigators (" +
            "id INTEGER PRIMARY KEY, " +
            "study INTEGER NOT NULL, " +
            "name TEXT NOT NULL, " +
            "institution TEXT NOT NULL, " +
            "wg TEXT NOT NULL, " + // Working group, because "group" is a keyword
            "pos TEXT NOT NULL, " + // Position
            "FOREIGN KEY (study) REFERENCES Study(id) ON DELETE CASCADE" +
            ");";
    private static final String CREATE_DATAREQUESTS = "CREATE TABLE IF NOT EXISTS DataRequest (" +
            "id INTEGER PRIMARY KEY, " +
            "study INTEGER NOT NULL, " +
            "datatype INTEGER NOT NULL, " +
            "granularity INTEGER NOT NULL, " +
            "frequency INTEGER NOT NULL, " +
            "FOREIGN KEY (study) REFERENCES Study(id) ON DELETE CASCADE" +
            ");";


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
                stmt.execute(CREATE_STUDIES);
                stmt.execute(CREATE_INVESTIGATORS);
                stmt.execute(CREATE_DATAREQUESTS);
            } catch (SQLException e) {
                // Something went wrong, print stacktrace
                e.printStackTrace();
            } finally {
                // Close statement to free up memory
                if (stmt != null) stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
    public void addStudyRequest(StudyRequest req) {
        assert isOpen();
        Savepoint before = null;
        try {
            before = mConnection.setSavepoint();
            PreparedStatement stmt = mConnection.prepareStatement(INSERT_STUDY);
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
            stmt.setString(13, req.risks);
            stmt.setInt   (14, req.verification);
            stmt.setString(15, RSA.encodeKey(req.privkey));
            stmt.setString(16, RSA.encodeKey(req.pubkey));
            stmt.setInt   (17, 1);  // TODO Change constants
            stmt.setBytes (18, serializeKeyPair(req.exchange.getKeypair()));
            stmt.setInt   (19, 1);  // TODO Change constants
            stmt.setBytes (20, req.queue);
            stmt.execute();
            mConnection.commit();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            if (before != null) try {
                mConnection.rollback(before);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public StudyRequest getStudyRequestByID(int id) {
        assert isOpen();
        return null;
    }

    @Override
    public List<StudyRequest> getStudyRequests() {
        assert isOpen();
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
