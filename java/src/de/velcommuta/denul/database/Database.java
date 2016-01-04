package de.velcommuta.denul.database;

import de.velcommuta.denul.data.GPSTrack;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.Shareable;
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
    StudyRequest getStudyRequestByID(long id);

    /**
     * Retrieve the ID of the study identified by the provided queue identifier
     * @param identifier The Queue identifier
     * @return The database ID of the study, or -1 if no study with that identifier exists
     */
    long getStudyIDByQueueIdentifier(byte[] identifier);

    /**
     * Retrieve all {@link StudyRequest}s from the database
     * @return A List of study requests
     */
    List<StudyRequest> getStudyRequests();

    /**
     * Add a new participant to a study
     * @param keys The keys that were negotiated with the participant
     * @param studyid The ID of the study the participant is associated with
     * @return The Database ID of the inserted participant
     */
    long addParticipant(KeySet keys, long studyid);

    /**
     * Get a List of all active participants
     * @return A List of participants
     */
    List<KeySet> getParticipants();

    /**
     * Get the database ID of a participant, identified by the KeySet
     * @param keys The KeySet
     * @return The database ID, or -1 if the keyset is not in the database
     */
    long getParticipantIDByKeySet(KeySet keys);

    /**
     * Add a GPS track to the database
     * @param track The GPS track
     * @param ownerid The Database ID of the owner (i.e. participant)
     */
    void addGPSTrack(GPSTrack track, long ownerid);

    /**
     * Get a List of all saved GPS tracks
     * @return A List of GPS Tracks
     */
    List<GPSTrack> getGPSTracks();

    /**
     * Get all GPS tracks shared by a sepcific participant
     * @param participantID The participants database ID
     * @return A List of GPSTracks, or an empty list if no tracks were shared
     */
    List<GPSTrack> getGPSTracksByParticipantID(long participantID);

    /**
     * Get all GPS tracks associated with a specific study
     * @param studyID The study ID
     * @return A list of all GPS tracks associated with that study, or an empty list if none exist
     */
    List<GPSTrack> getGPSTracksByStudyID(long studyID);

    /**
     * Get a List of Shareables shared by a specific study participant, identified by its ID
     * @param participantID The Participant ID
     * @return A List of Shareables, or an empty List if no shares have been sent yet
     */
    List<Shareable> getDataByParticipantID(long participantID);

    /**
     * Get a List of all Shareables sent for a specific study
     * @param studyid The Study ID
     * @return A List of shareables, or an empty list if no shareables have been received for the study
     */
    List<Shareable> getDataByStudyID(long studyid);
}
