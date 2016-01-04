package de.velcommuta.denul.database;

import de.velcommuta.denul.data.*;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.*;

/**
 * Test the SQLite database backend
 */
public class SQLiteDatabaseTest extends TestCase {
    private SQLiteDatabase mDB;

    @Before
    public void setUp() {
        mDB = new SQLiteDatabase("test.db");
    }


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
     * Test the adding and retrieving of study requests
     */
    public void testAddReadStudyRequest() {
        StudyRequest req = StudyRequestTest.getRandomStudyRequest();
        long rv = mDB.addStudyRequest(req);
        assertTrue(rv >= 0);
        StudyRequest read = mDB.getStudyRequestByID(rv);
        assertEquals(req, read);
    }


    /**
     * Test the adding and retrieving of study requests
     */
    public void testAddReadMultiple() {
        StudyRequest req1 = StudyRequestTest.getRandomStudyRequest();
        long rv1 = mDB.addStudyRequest(req1);
        assertTrue(rv1 >= 0);
        StudyRequest req2 = StudyRequestTest.getRandomStudyRequest();
        long rv2 = mDB.addStudyRequest(req2);
        assertTrue(rv2 >= 0);
        List<StudyRequest> reqs = new LinkedList<>();
        reqs.add(req1);
        reqs.add(req2);
        List<StudyRequest> reps = mDB.getStudyRequests();
        assertEquals(reps, reqs);
    }

    /**
     * Test if the getIDByQueue function works as intended
     */
    public void testGetIDByQueue() {
        StudyRequest req = StudyRequestTest.getRandomStudyRequest();
        long rv = mDB.addStudyRequest(req);
        long rv2 = mDB.getStudyIDByQueueIdentifier(req.queue);
        assertEquals(rv, rv2);
        long rv3 = mDB.getStudyIDByQueueIdentifier(new byte[] {0x00, 0x00});
        assertEquals(rv3, -1);
    }

    /**
     * Test if adding study participants works
     */
    public void testInsertQueryRetrieveParticipant() {
        StudyRequest req = StudyRequestTest.getRandomStudyRequest();
        long rv = mDB.addStudyRequest(req);
        byte[] key1 = new byte[32];
        byte[] key2 = new byte[32];
        byte[] ctr1 = new byte[32];
        byte[] ctr2 = new byte[32];
        new Random().nextBytes(key1);
        new Random().nextBytes(key2);
        new Random().nextBytes(ctr1);
        new Random().nextBytes(ctr2);
        KeySet ks = new KeySet(key1, key2, ctr1, ctr2, true);
        long ksid = mDB.addParticipant(ks, rv);
        // Retrieve ID of participant
        long ksid2 = mDB.getParticipantIDByKeySet(ks);
        assertEquals(ksid, ksid2);
        // Retrieve List of Participants (should only be this one)
        List<KeySet> list = mDB.getParticipants();
        KeySet first = list.get(0);
        assertTrue(Arrays.equals(ks.getInboundCtr(), first.getInboundCtr()));
        assertTrue(Arrays.equals(ks.getOutboundCtr(), first.getOutboundCtr()));
        assertTrue(Arrays.equals(ks.getInboundKey(), first.getInboundKey()));
        assertTrue(Arrays.equals(ks.getOutboundKey(), first.getOutboundKey()));
        assertEquals(ksid, first.getID());
    }

    /**
     * Test the insert of location data
     */
    public void testInsertLocationLog() {
        // prepare study
        StudyRequest req = StudyRequestTest.getRandomStudyRequest();
        long rv = mDB.addStudyRequest(req);
        // Prepare owner
        byte[] key1 = new byte[32];
        byte[] key2 = new byte[32];
        byte[] ctr1 = new byte[32];
        byte[] ctr2 = new byte[32];
        new Random().nextBytes(key1);
        new Random().nextBytes(key2);
        new Random().nextBytes(ctr1);
        new Random().nextBytes(ctr2);
        KeySet ks = new KeySet(key1, key2, ctr1, ctr2, true);
        long part = mDB.addParticipant(ks, rv);
        // Prepare data
        List<Location> loclist = new LinkedList<>();
        Location loc = new Location();
        loc.setLatitude(0);
        loc.setLongitude(1);
        loc.setTime(10f);
        loclist.add(loc);
        GPSTrack track = new GPSTrack(loclist, "bla", GPSTrack.VALUE_CYCLING, 0, 1, "GMT+1", 1000.0f);
        // Perform insert
        mDB.addGPSTrack(track, part);
    }
}