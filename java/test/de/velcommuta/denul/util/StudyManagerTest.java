package de.velcommuta.denul.util;

import com.google.protobuf.ByteString;
import de.velcommuta.denul.crypto.*;
import de.velcommuta.denul.data.*;
import de.velcommuta.denul.database.SQLiteDatabase;
import de.velcommuta.denul.database.SQLiteDatabaseTest;
import de.velcommuta.denul.networking.Connection;
import de.velcommuta.denul.networking.ProtobufProtocol;
import de.velcommuta.denul.networking.Protocol;
import de.velcommuta.denul.networking.TLSConnection;
import de.velcommuta.denul.networking.protobuf.study.StudyMessage;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;

import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Test cases for the StudyManager
 */
public class StudyManagerTest extends TestCase {
    private SQLiteDatabase mDB;

    /**
     * Setup function
     */
    @Before
    public void setUp() {
        mDB = new SQLiteDatabase("test.db");
    }


    /**
     * Teardown function
     */
    @After
    public void tearDown() {
        mDB.close();
        mDB = null;
        // Delete database file
        File f = new File("test.db");
        if (f.isFile()) {
            assertTrue(f.delete());
        }
    }

    /**
     * Test adding and deleting studies
     */
    public void testAddDeleteStudy() {
        StudyRequest req = StudyRequestTest.getRandomStudyRequest();
        assertTrue(StudyManager.registerStudy(req, mDB));
        assertEquals(StudyManager.getMyStudies(mDB).size(), 1);
        assertTrue(StudyManager.deleteStudy(req, mDB));
        assertEquals(StudyManager.getMyStudies(mDB).size(), 0);
    }


    /**
     * Test a full combination: Setting up a study, adding participants, retrieving data, and deleting the study
     * @throws IllegalBlockSizeException If RSA feels like it
     */
    public void testFullCombination() throws IllegalBlockSizeException {
        try {
            Connection c = new TLSConnection(Config.getServerHost(), Config.getServerPort());
            Protocol p = new ProtobufProtocol();
            p.connect(c);

            // Create and register study
            StudyRequest req = StudyRequestTest.getRandomStudyRequest(4096);
            assertTrue(StudyManager.registerStudy(req, mDB));

            // Create two participants
            KeyExchange kex1 = new ECDHKeyExchange();
            KeyExchange kex2 = new ECDHKeyExchange();

            // Upload first participant
            StudyMessage.StudyJoin.Builder join1 = StudyMessage.StudyJoin.newBuilder();
            join1.setQueueIdentifier(ByteString.copyFrom(req.queue));
            join1.setKexAlgorithm(StudyMessage.StudyJoin.KexAlgo.KEX_ECDH_CURVE25519);
            join1.setKexData(ByteString.copyFrom(kex1.getPublicKexData()));
            byte[] ciphertext = RSA.encryptRSA(join1.build().toByteArray(), req.pubkey);
            DataBlock block = new DataBlock(req.queue, ciphertext, req.queue);
            assertEquals(p.put(block), Protocol.PUT_OK);

            // Upload second participant
            StudyMessage.StudyJoin.Builder join2 = StudyMessage.StudyJoin.newBuilder();
            join2.setQueueIdentifier(ByteString.copyFrom(req.queue));
            join2.setKexAlgorithm(StudyMessage.StudyJoin.KexAlgo.KEX_ECDH_CURVE25519);
            join2.setKexData(ByteString.copyFrom(kex2.getPublicKexData()));
            ciphertext = RSA.encryptRSA(join2.build().toByteArray(), req.pubkey);
            block = new DataBlock(req.queue, ciphertext, req.queue);
            assertEquals(p.put(block), Protocol.PUT_OK);

            kex1.putPartnerKexData(req.exchange.getPublicKexData());
            kex2.putPartnerKexData(req.exchange.getPublicKexData());

            byte[] secret1 = kex1.getAgreedKey();
            byte[] secret2 = kex2.getAgreedKey();

            HKDFKeyExpansion exp1 = new HKDFKeyExpansion(secret1);
            HKDFKeyExpansion exp2 = new HKDFKeyExpansion(secret2);

            KeySet key1 = exp1.expand(false);
            KeySet key2 = exp2.expand(false);

            // TODO Also share some data?

            StudyManager.updateAllStudyData(mDB);

            List<KeySet> participants = StudyManager.getStudyParticipants(req, mDB);

            assertEquals(participants.size(), 2);
            for (KeySet ks : participants) {
                assertTrue(Arrays.equals(ks.getInboundKey(), key1.getOutboundKey()) ||Arrays.equals(ks.getInboundKey(), key2.getOutboundKey()));
                assertTrue(Arrays.equals(ks.getInboundCtr(), key1.getOutboundCtr()) ||Arrays.equals(ks.getInboundCtr(), key2.getOutboundCtr()));
                assertTrue(Arrays.equals(ks.getOutboundKey(), key1.getInboundKey()) ||Arrays.equals(ks.getOutboundKey(), key2.getInboundKey()));
                assertTrue(Arrays.equals(ks.getOutboundCtr(), key1.getInboundCtr()) ||Arrays.equals(ks.getOutboundCtr(), key2.getInboundCtr()));
            }

            // Unregister and delete study
            assertTrue(StudyManager.deleteStudy(req, mDB));
        } catch (IOException | IllegalBlockSizeException e) {
            e.printStackTrace();
            fail();
        }
    }
}