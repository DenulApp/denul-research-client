package de.velcommuta.denul.database;

import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.data.StudyRequestTest;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

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
            f.delete();
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
}