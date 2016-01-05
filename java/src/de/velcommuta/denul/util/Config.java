package de.velcommuta.denul.util;

/**
 * Provides configuration data and settings
 */
public class Config {
    /**
     * Getter for the server the client should connect to
     * @return The DNS name of the server to connect to
     */
    public static String getServerHost() {
        return "denul.velcommuta.de";
    }

    /**
     * Getter for the port the server is running on
     * @return The port the server is running on
     */
    public static int getServerPort() {
        return 5566;
    }
}
