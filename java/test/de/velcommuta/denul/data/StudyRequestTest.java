package de.velcommuta.denul.data;

import de.velcommuta.denul.crypto.ECDHKeyExchange;
import de.velcommuta.denul.crypto.RSA;
import de.velcommuta.denul.networking.protobuf.study.StudyMessage;
import de.velcommuta.denul.util.AsyncKeyGenerator;
import de.velcommuta.denul.util.FormatHelper;
import junit.framework.TestCase;
import org.junit.Test;

import java.security.KeyPair;
import java.util.Random;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the serialization and signature functions of the StudyRequest
 */
public class StudyRequestTest extends TestCase {
    /**
     * Test the signature and serialization functions
     * @throws Exception Whenever it bloody feels like it
     */
    public void testSignAndSerialize() throws Exception {
        // Start key generation in background
        FutureTask<KeyPair> rsagen = AsyncKeyGenerator.generateRSA(1024);
        FutureTask<ECDHKeyExchange> ecdhgen = AsyncKeyGenerator.generateECDH();

        StudyRequest req = new StudyRequest();

        // Random request data
        req.name = getRandomString();
        req.institution = getRandomString();
        req.webpage = getRandomString();
        req.description = getRandomString();
        req.purpose = getRandomString();
        req.procedures = getRandomString();
        req.risks = getRandomString();
        req.benefits = getRandomString();
        req.payment = getRandomString();
        req.conflicts = getRandomString();
        req.confidentiality = getRandomString();
        req.participationAndWithdrawal = getRandomString();
        req.rights = getRandomString();

        req.verification = StudyRequest.VERIFY_DNS;
        req.queue = new byte[] {0x00};

        // Random investigator
        StudyRequest.Investigator inv = new StudyRequest.Investigator();
        inv.position = getRandomString();
        inv.name = getRandomString();
        inv.group = getRandomString();
        inv.institution = getRandomString();
        req.investigators.add(inv);

        // Add datarequest
        StudyRequest.DataRequest dr = new StudyRequest.DataRequest();
        dr.granularity = StudyRequest.DataRequest.GRANULARITY_FINE;
        dr.type = StudyRequest.DataRequest.TYPE_GPS;
        dr.frequency = 0;
        req.requests.add(dr);

        // Add keys
        req.exchange = ecdhgen.get();
        KeyPair keys = rsagen.get();
        req.pubkey = keys.getPublic();
        req.privkey = keys.getPrivate();

        // Serialize
        byte[] serialized = req.signAndSerialize();
        assertNotNull(serialized);

        // Deserialize
        StudyMessage.StudyWrapper wrapper = StudyMessage.StudyWrapper.parseFrom(serialized);
        assertNotNull(wrapper);
        assertTrue(RSA.verify(wrapper.getMessage().toByteArray(), wrapper.getSignature().toByteArray(), keys.getPublic()));

        StudyMessage.Study study = StudyMessage.Study.parseFrom(wrapper.getMessage().toByteArray());
        assertNotNull(study);
    }

    /**
     * Helper function to generate a random String
     * @return A random string
     */
    private String getRandomString() {
        byte[] rand = new byte[20];
        new Random().nextBytes(rand);
        return FormatHelper.bytesToHex(rand);
    }
}