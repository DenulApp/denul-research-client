package de.velcommuta.denul.database;

import de.velcommuta.denul.data.StudyRequest;

import java.util.List;

/**
 * Interface for database implementations - must be implemented by all database connectors
 */
public interface Database {
    /**
     * Close the database, in case the underlying implementation requires an explicit close.
     */
    void close();

    /**
     * Add a {@link StudyRequest} to the database
     * @param studyRequest The study request
     * @return The ID of the inserted record, or -1 in case of an error
     */
    long addStudyRequest(StudyRequest studyRequest);

    /**
     * Retrieve a specific {@link StudyRequest} by its ID
     * @param id The ID
     * @return The study request
     */
    StudyRequest getStudyRequestByID(int id);

    /**
     * Retrieve all {@link StudyRequest}s from the database
     * @return A List of study requests
     */
    List<StudyRequest> getStudyRequests();
}
