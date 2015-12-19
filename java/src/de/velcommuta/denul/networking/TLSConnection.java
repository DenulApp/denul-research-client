package de.velcommuta.denul.networking;

import java.io.BufferedInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.*;

/**
 * A TCP connection using TLS to communicate with the server.
 */
public class TLSConnection implements Connection {

    private static final Logger logger = Logger.getLogger(TLSConnection.class.getName());


    SSLSocket mSocket;

    /**
     * Establish a TCP connection protected by TLS.
     * @param host Either the IP or the FQDN of the server to connect to
     * @param port The port number to connect to
     * @throws IOException If the underlying socket throws it
     * @throws UnknownHostException If the underlying socket throws it
     * @throws SSLHandshakeException If the certificate hostname validation fails
     * Code partially based on https://docs.fedoraproject.org/en-US/Fedora_Security_Team/1/html/Defensive_Coding/sect-Defensive_Coding-TLS-Client-OpenJDK.html
     */
    public TLSConnection(String host, int port) throws IOException, UnknownHostException, SSLHandshakeException {
        logger.fine("TLSConnection: Establishing connection to " + host + ":" + port);
        // Get SSL context
        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("TLSv1.2", "SunJSSE");
        } catch (NoSuchAlgorithmException e) {
            // No TLS 1.2 available. Fall back to TLS 1.0
            try {
                ctx = SSLContext.getInstance("TLSv1", "SunJSSE");
            } catch (NoSuchAlgorithmException | NoSuchProviderException e2) {
                throw new IOError(e2);
            }
        } catch (NoSuchProviderException e) {
            throw new IOError(e);
        }
        try {
            // initiate Context
            ctx.init(null, null, null);
        } catch (KeyManagementException e) {
            throw new IOError(e);
        }
        // Get SSL Parameters
        SSLParameters params = ctx.getDefaultSSLParameters();
        // Set allowed protocols
        ArrayList<String> protocols = new ArrayList<String>(
                Arrays.asList(params.getProtocols()));
        // No SSLv2 compatible HELLO
        protocols.remove("SSLv2Hello");
        // No SSLv3
        protocols.remove("SSLv3");
        // Set the protocols
        params.setProtocols(protocols.toArray(new String[protocols.size()]));
        // Bad ciphers we don't want to see used:
        String[] badAlgos = {
                "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
                "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA"
        };
        // Get ciphersuite
        ArrayList<String> ciphers = new ArrayList<>(
                Arrays.asList(params.getCipherSuites()));
        // Remove bad algorithms
        ciphers.removeAll(Arrays.asList(badAlgos));
        // Set the cipher suite
        params.setCipherSuites(ciphers.toArray(new String[ciphers.size()]));
        // Enable verification
        params.setEndpointIdentificationAlgorithm("HTTPS");
        // Get SSL Socket factory
        SocketFactory factory = SSLSocketFactory.getDefault();
        // Create a socket and connect to the host and port, throwing an exception if anything
        // goes wrong
        mSocket = (SSLSocket) factory.createSocket(host, port);
        // Set the parameters
        mSocket.setSSLParameters(params);
        // Start the handshake
        mSocket.startHandshake();
        // Get an SSLSession object
        SSLSession s = mSocket.getSession();
        logger.fine("TLSConnection: Connection established using " + s.getProtocol() + " (" +  s.getCipherSuite() + ")");
    }

    @Override
    public byte[] transceive(byte[] message) throws IOException {
        // Get an output stream from the socket (to send data over)
        OutputStream out = mSocket.getOutputStream();
        // Get an input stream from the socket (to receive data with)
        BufferedInputStream in = new BufferedInputStream(mSocket.getInputStream());

        // Prepare the byte[] with the length information of the message
        byte[] len = ByteBuffer.allocate(4).putInt(message.length).array();
        // Combine length and message into one byte[]
        byte[] fullmsg = new byte[4 + message.length];
        System.arraycopy(len, 0, fullmsg, 0, len.length);
        System.arraycopy(message, 0, fullmsg, len.length, message.length);

        // Send the message over the socket
        out.write(fullmsg);
        out.flush();
        logger.fine("transceive: Message sent");

        // Receive the reply - Receive the length of the reply
        byte[] lenbytes = new byte[4];
        // Read 4 bytes from the wire (in a loop to make sure that we actually get 4 bytes)
        int rcvlen = 0;
        do {
            rcvlen += in.read(lenbytes, rcvlen, 4-rcvlen);
        } while (rcvlen < 4);
        // Parse the received bytes into an integer
        int replylen = ByteBuffer.wrap(lenbytes).getInt();
        logger.fine("transceive: Reply has " + replylen + " bytes");

        // Receive the body of the reply (again, in a loop to make sure we get it all)
        byte[] replyBytes = new byte[replylen];
        rcvlen = 0;
        do {
            rcvlen += in.read(replyBytes, rcvlen, replylen-rcvlen);
        } while (rcvlen < replylen);

        // Return received bytes
        logger.fine("transceive: Reply received, returning");
        return replyBytes;
    }

    @Override
    public void close() throws IOException {
        if (mSocket.isConnected()) {
            logger.fine("close: Closing open socket");
            mSocket.close();
        } else {
            logger.warning("close: Trying to close socket that is not open");
        }
    }

    @Override
    public boolean isOpen() {
        return mSocket.isConnected();
    }
}
