package de.velcommuta.denul.database;

import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.data.StudyRequestTest;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    }


    /**
     * Test the adding of study requests
     */
    public void testAddStudyRequest() {
        StudyRequest req = StudyRequestTest.getRandomStudyRequest();
        mDB.addStudyRequest(req);
    }

    /**
     * Test the "get by ID" function
     */
    public void testGetStudyRequestByID() {
        // TODO
        assertTrue(true);
    }

    /**
     * Test the "get all" function
     */
    public void testGetStudyRequests() {
        // TODO
        assertTrue(true);
    }
}