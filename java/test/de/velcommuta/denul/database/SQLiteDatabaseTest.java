package de.velcommuta.denul.database;

import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.data.StudyRequestTest;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

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
     * Test the "get all" function
     */
    public void testGetStudyRequests() {
        // TODO
        assertTrue(true);
    }
}