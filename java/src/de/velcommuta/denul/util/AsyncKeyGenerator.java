package de.velcommuta.denul.util;

import de.velcommuta.denul.crypto.ECDHKeyExchange;
import de.velcommuta.denul.crypto.RSA;

import java.security.KeyPair;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Asynchronously generate cryptographic keys in the background
 */
public class AsyncKeyGenerator {
    /**
     * Create a task that generates an RSA key in the background and makes it possible to retrieve it from the returned
     * object. Call {@link FutureTask#isDone()} to determine if the computations have been completed, and call
     * {@link FutureTask#get()} to retrieve the result (will block until the result is available)
     * @param bitness Bitstrength of the generated key
     * @return A {@link FutureTask} which will compute and return an RSA keypair
     * Code based in part on http://stackoverflow.com/a/27955299/1232833
     */
    public static FutureTask<KeyPair> generateRSA(final int bitness) {
        FutureTask<KeyPair> task = new FutureTask<>(
                new Callable<KeyPair>() {
                    @Override
                    public KeyPair call() throws Exception {
                        return RSA.generateRSAKeypair(bitness);
                    }
                }
        );
        new Thread(task).start();
        return task;
    }


    /**
     * Create a task that generates an ECDH key in the background and makes it possible to retrieve it from the returned
     * object. Call {@link FutureTask#isDone()} to determine if the computations have been completed, and call
     * {@link FutureTask#get()} to retrieve the result (will block until the result is available)
     * @return A {@link FutureTask} which will compute and return an ECDH Key exchange
     * Code based in part on http://stackoverflow.com/a/27955299/1232833
     */
    public static FutureTask<ECDHKeyExchange> generateECDH() {
        FutureTask<ECDHKeyExchange> task = new FutureTask<>(
                new Callable<ECDHKeyExchange>() {
                    @Override
                    public ECDHKeyExchange call() throws Exception {
                        // Technically, it's overkill to do this in a background task, as the generation takes <1 second
                        // on a somewhat modern machine, but we'll do it anyway
                        return new ECDHKeyExchange();
                    }
                }
        );
        new Thread(task).start();
        return task;
    }
}
