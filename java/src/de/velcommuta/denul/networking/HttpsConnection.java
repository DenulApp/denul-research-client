package de.velcommuta.denul.networking;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLProtocolException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * HTTPS connection class offering static methods to check for the existance of specific HTTPS URLs and retrieve their
 * content. NOT an implementation of the {@link Connection} interface, as it has different goals
 */
public class HttpsConnection {
    /**
     * Check if a specified URL exists and is reachable via HTTPS
     * @param addr The URL
     * @return True if the URL is a valid URL and does not 404, false if the URL is valid, but gives a 404 or the Host
     * cannot be found.
     * @throws MalformedURLException If the provided string is not a valid URL
     * @throws SSLProtocolException If the cert is invalid
     * @throws UnknownHostException If the URL seems valid, but no such domain exists
     */
    public static boolean exists(String addr) throws MalformedURLException, SSLProtocolException, UnknownHostException {
        try {
            URL url = new URL(addr);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            // If the exception was a MalformedURLException or SSLProtocolException, re-raise them, we're interested
            if (e instanceof MalformedURLException) throw (MalformedURLException) e;
            if (e instanceof SSLProtocolException) throw (SSLProtocolException) e;
            if (e instanceof UnknownHostException) throw (UnknownHostException) e;
            e.printStackTrace();
        } catch (ClassCastException e) {
            // The URL was an HTTP URL => Invalid
            throw new IllegalArgumentException("URL was http, not https");
        }
        return false;
    }
}
