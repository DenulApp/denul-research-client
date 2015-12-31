package de.velcommuta.denul.data;

import com.google.protobuf.ByteString;
import de.velcommuta.denul.crypto.KeyExchange;
import de.velcommuta.denul.crypto.RSA;
import de.velcommuta.denul.networking.protobuf.study.StudyMessage;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

/**
 * Data holder class for study requests
 */
public class StudyRequest {
    // Static constants
    public static final int VERIFY_UNKNOWN = 0;
    public static final int VERIFY_FILE = 1;
    public static final String VERIFY_FILE_TITLE = "(easy) File";
    public static final String VERIFY_FILE_DESC_SHORT = "You will have to put a file into a specific location relative to your study URL";
    public static final String VERIFY_FILE_DESC_LONG = "To authenticate your request, you will have to put the following string into its own line in the file at %s:\n%s";
    public static final int VERIFY_META = 2;
    public static final String VERIFY_META_TITLE = "(advanced) <meta>-Tag";
    public static final String VERIFY_META_DESC_SHORT = "You will have to add a special <meta> tag to the source code of your study website";
    public static final String VERIFY_META_DESC_LONG = "To authenticate your request, you will have to put the following <meta> tag into the <head> of the HTML document at %s:\n<meta name='study-key' content='%s'>";
    public static final int VERIFY_DNS = 3;
    public static final String VERIFY_DNS_TITLE = "(expert) DNS Entry";
    public static final String VERIFY_DNS_DESC_SHORT = "You will have to add a TXT record to the DNS entries of your domain";
    public static final String VERIFY_DNS_DESC_LONG = "To authenticate your request, you will have to put the following value into the TXT record of the domain %s:\n%s \nNote that you can only have one such record at a time per domain name.";
    // FIXME Debugging helper, remove
    public static final String VERIFY_SKIP = "(debug) Skip";
    public static final String VERIFY_SKIP_DESC_SHORT = "Skip verification (debugging helper, remove)";

    public static final HashMap<String, String> verificationOptions = new HashMap<>();
    public static final HashMap<String, String> verificationDetails = new HashMap<>();
    static {
        // Fill short descriptions
        verificationOptions.put(VERIFY_FILE_TITLE, VERIFY_FILE_DESC_SHORT);
        verificationOptions.put(VERIFY_META_TITLE, VERIFY_META_DESC_SHORT);
        verificationOptions.put(VERIFY_DNS_TITLE, VERIFY_DNS_DESC_SHORT);
        // FIXME Debugging helper, remove
        verificationOptions.put(VERIFY_SKIP, VERIFY_SKIP_DESC_SHORT);
        // Fill long descriptions
        verificationDetails.put(VERIFY_FILE_TITLE, VERIFY_FILE_DESC_LONG);
        verificationDetails.put(VERIFY_META_TITLE, VERIFY_META_DESC_LONG);
        verificationDetails.put(VERIFY_DNS_TITLE, VERIFY_DNS_DESC_LONG);
    }

    // Value holder fields
    public long id;
    public String name;
    public String institution;
    public String webpage;
    public String description;
    public String purpose;
    public String procedures;
    public String risks;
    public String benefits;
    public String payment;
    public String conflicts;
    public String confidentiality;
    public String participationAndWithdrawal;
    public String rights;
    public List<Investigator> investigators = new LinkedList<>();
    public List<DataRequest> requests = new LinkedList<>();
    public int verification = VERIFY_UNKNOWN;

    // Cryptographic material
    public PublicKey pubkey;
    public PrivateKey privkey;
    public KeyExchange exchange;

    // Queue identifier on the server
    public byte[] queue;

    /**
     * Randomize the queue identifier
     */
    public void randomizeQueueIdentifier() {
        queue = new byte[16];
        new Random().nextBytes(queue);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Name: ");
        builder.append(name);
        builder.append("\n");

        builder.append("Institution: ");
        builder.append(institution);
        builder.append("\n");

        builder.append("Web page: ");
        builder.append(webpage);
        builder.append("\n\n");

        builder.append("Description:\n");
        builder.append(description);
        builder.append("\n\n");

        builder.append("Purpose:\n");
        builder.append(purpose);
        builder.append("\n\n");

        builder.append("Procedures:\n");
        builder.append(procedures);
        builder.append("\n\n");

        builder.append("Risks:\n");
        builder.append(risks);
        builder.append("\n\n");

        builder.append("Benefits:\n");
        builder.append(benefits);
        builder.append("\n\n");

        builder.append("Payment:\n");
        builder.append(payment);
        builder.append("\n\n");

        builder.append("Conflicts of Interest:\n");
        builder.append(conflicts);
        builder.append("\n\n");

        builder.append("Confidentiality:\n");
        builder.append(confidentiality);
        builder.append("\n\n");

        builder.append("Participation and Withdrawal:\n");
        builder.append(participationAndWithdrawal);
        builder.append("\n\n");

        builder.append("Subjects Rights:\n");
        builder.append(rights);
        builder.append("\n\n");

        for (int i = 0; i < investigators.size(); i++) {
            builder.append("Investigator #");
            builder.append(i+1);
            builder.append(":\n");
            builder.append(investigators.get(i).toString());
            builder.append("\n");
        }

        builder.append("\n");
        for (int i = 0; i < requests.size(); i++) {
            builder.append("Data Request #");
            builder.append(i+1);
            builder.append(":\n");
            builder.append(requests.get(i).toString());
            builder.append("\n");
        }

        return builder.toString();
    }

    /**
     * Data holder class for Investigators associated with a study
     */
    public static class Investigator {
        public String name;
        public String institution;
        public String group;
        public String position;

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Investigator Name: ");
            builder.append(name);

            builder.append("\nInstitution: ");
            builder.append(institution);

            builder.append("\nGroup: ");
            builder.append(group);

            builder.append("\nPosition: ");
            builder.append(position);

            builder.append("\n");
            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Investigator)) return false;
            Investigator other = (Investigator) o;
            return ((other.name.equals(name)) &&
                    (other.institution.equals(institution)) &&
                    (other.group.equals(group)) &&
                    (other.position.equals(position)));
        }

        /**
         * Serialize the Investigator object into a protocol buffer and return it
         * @return A protocol buffer {@link de.velcommuta.denul.networking.protobuf.study.StudyMessage.StudyCreate.Investigator}
         * object
         */
        public StudyMessage.StudyCreate.Investigator toProtobuf() {
            assert name != null;
            assert institution != null;
            assert group != null;
            assert position != null;

            StudyMessage.StudyCreate.Investigator.Builder builder = StudyMessage.StudyCreate.Investigator.newBuilder();
            builder.setName(name);
            builder.setInstitution(institution);
            builder.setGroup(group);
            builder.setPosition(position);
            return builder.build();
        }
    }

    /**
     * Data holder class for information about what data is requested in what granularity
     */
    public static class DataRequest {
        public static final String[] TYPES = {"GPS Tracks"};
        public static final int TYPE_GPS = 0;

        public static final String[] GRANULARITIES_GPS = {"Full GPS tracks", "Duration, time and distance only"};
        public static final int GRANULARITY_FINE = 0;
        public static final int GRANULARITY_COARSE = 1;
        public static final int GRANULARITY_VERY_COARSE = 2;

        public Integer type;
        public Integer granularity;
        public Integer frequency;

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Data type: ");
            if (type != null) {
                builder.append(TYPES[type]);
            } else {
                builder.append("unset");
            }

            builder.append("\nGranularity: ");
            if (granularity != null) {
                if (type == TYPE_GPS) {
                    builder.append(GRANULARITIES_GPS[granularity]);
                }
            } else {
                builder.append("unset");
            }

            if (frequency != null) {
                builder.append("\nFrequency: Updated every ");
                builder.append(frequency);
                builder.append(" hour(s)");
            } else {
                builder.append("\nFrequency: unset");
            }

            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof DataRequest)) return false;
            DataRequest other = (DataRequest) o;
            return other.frequency.equals(frequency) &&
                    other.type.equals(type) &&
                    other.granularity.equals(granularity);
        }


        /**
         * Serialize the object into a protocol buffer
         * @return A protocol buffer representation of the DataRequest
         */
        public StudyMessage.StudyCreate.DataRequest toProtobuf() {
            assert type != null;
            assert granularity != null;
            assert frequency != null;

            // Get builder and add values
            StudyMessage.StudyCreate.DataRequest.Builder builder = StudyMessage.StudyCreate.DataRequest.newBuilder();
            switch (type) {
                case TYPE_GPS:
                    builder.setDatatype(StudyMessage.StudyCreate.DataType.DATA_GPS_TRACK);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown data type");
            }
            switch (granularity) {
                case GRANULARITY_FINE:
                    builder.setGranularity(StudyMessage.StudyCreate.DataGranularity.GRAN_FINE);
                    break;
                case GRANULARITY_COARSE:
                    builder.setGranularity(StudyMessage.StudyCreate.DataGranularity.GRAN_COARSE);
                    break;
                case GRANULARITY_VERY_COARSE:
                    builder.setGranularity(StudyMessage.StudyCreate.DataGranularity.GRAN_VERY_COARSE);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown granularity");
            }
            builder.setFrequency(frequency);

            return builder.build();
        }
    }

    /**
     * Serialize a StudyRequest into a byte[] containing a {@link de.velcommuta.denul.networking.protobuf.study.StudyMessage.StudyWrapper}
     * with a signed serialized {@link de.velcommuta.denul.networking.protobuf.study.StudyMessage.StudyCreate} message inside,
     * @return A {@link de.velcommuta.denul.networking.protobuf.study.StudyMessage.StudyWrapper} containing the data structure
     */
    public StudyMessage.StudyWrapper signAndSerialize() {
        // Ensure all fields are set
        assert name != null;
        assert institution != null;
        assert webpage != null;
        assert description != null;
        assert purpose != null;
        assert procedures != null;
        assert risks != null;
        assert benefits != null;
        assert payment != null;
        assert conflicts != null;
        assert confidentiality != null;
        assert participationAndWithdrawal != null;
        assert rights != null;
        assert investigators.size() != 0;
        assert requests.size() != 0;
        assert pubkey != null;
        assert privkey != null;
        assert exchange != null;

        // Get a builder
        StudyMessage.StudyCreate.Builder builder = StudyMessage.StudyCreate.newBuilder();
        builder.setStudyName(name);
        builder.setInstitution(institution);
        builder.setWebpage(webpage);
        builder.setDescription(description);
        builder.setPurpose(purpose);
        builder.setProcedures(procedures);
        builder.setRisks(risks);
        builder.setBenefits(benefits);
        builder.setPayment(payment);
        builder.setConflicts(conflicts);
        builder.setConfidentiality(confidentiality);
        builder.setParticipationAndWithdrawal(participationAndWithdrawal);
        builder.setRights(rights);

        for (Investigator inv : investigators) {
            builder.addInvestigators(inv.toProtobuf());
        }
        for (DataRequest req : requests) {
            builder.addDataRequest(req.toProtobuf());
        }

        builder.setPublicKey(ByteString.copyFrom(pubkey.getEncoded()));
        builder.setPublicKeyAlgo(StudyMessage.StudyCreate.PubkeyAlgo.PK_RSA);
        builder.setKexData(ByteString.copyFrom(exchange.getPublicKexData()));
        builder.setKexAlgorithm(StudyMessage.StudyCreate.KexAlgo.KEX_ECDH_CURVE25519);

        switch (verification) {
            case VERIFY_DNS:
                builder.setVerificationStrategy(StudyMessage.StudyCreate.VerificationStrategy.VF_DNS_TXT);
                break;
            case VERIFY_FILE:
                builder.setVerificationStrategy(StudyMessage.StudyCreate.VerificationStrategy.VF_FILE);
                break;
            case VERIFY_META:
                builder.setVerificationStrategy(StudyMessage.StudyCreate.VerificationStrategy.VF_META);
                break;
            default:
                throw new IllegalArgumentException("Unknown verification strategy");
        }

        builder.setQueueIdentifier(ByteString.copyFrom(queue));  // TODO Set queue somewhere

        byte[] serialized = builder.build().toByteArray();

        StudyMessage.StudyWrapper.Builder wrapper = StudyMessage.StudyWrapper.newBuilder();
        wrapper.setMessage(ByteString.copyFrom(serialized));
        byte[] signature = RSA.sign(serialized, privkey);
        assert signature != null;
        wrapper.setSignature(ByteString.copyFrom(signature));
        wrapper.setType(StudyMessage.StudyWrapper.MessageType.MSG_STUDYCREATE);

        return wrapper.build();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof StudyRequest)) return false;
        StudyRequest other = (StudyRequest) o;
        return (other.id == id) &&
                (other.name.equals(name)) &&
                (other.institution.equals(institution)) &&
                (other.webpage.equals(webpage)) &&
                (other.description.equals(description)) &&
                (other.purpose.equals(purpose)) &&
                (other.procedures.equals(procedures)) &&
                (other.risks.equals(risks)) &&
                (other.benefits.equals(benefits)) &&
                (other.payment.equals(payment)) &&
                (other.conflicts.equals(conflicts)) &&
                (other.confidentiality.equals(confidentiality)) &&
                (other.participationAndWithdrawal.equals(participationAndWithdrawal)) &&
                (other.rights.equals(rights)) &&
                (other.verification == verification )&&
                (other.pubkey.equals(pubkey)) &&
                (other.privkey.equals(privkey)) &&
                (Arrays.equals(other.exchange.getPublicKexData(), exchange.getPublicKexData())) &&
                (Arrays.equals(exchange.getKeypair().getPrivate().getEncoded(),
                        other.exchange.getKeypair().getPrivate().getEncoded())) &&
                (Arrays.equals(other.queue, queue)) &&
                (other.investigators.equals(investigators)) &&
                (other.requests.equals(requests));
    }
}
