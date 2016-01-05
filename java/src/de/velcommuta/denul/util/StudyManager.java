package de.velcommuta.denul.util;

import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.database.Database;
import de.velcommuta.denul.networking.Connection;
import de.velcommuta.denul.networking.ProtobufProtocol;
import de.velcommuta.denul.networking.Protocol;
import de.velcommuta.denul.networking.TLSConnection;

import java.io.IOException;

/**
 * Class providing static functions to perform study management. All-in-one solution for network- and database side of
 * study management, data retrieval, and so on
 */
public class StudyManager {
    /**
     * Register a Study on the server and add it to the local database
     * @param req The study request
     * @param db The database handler
     */
    public static void registerStudy(StudyRequest req, Database db) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
