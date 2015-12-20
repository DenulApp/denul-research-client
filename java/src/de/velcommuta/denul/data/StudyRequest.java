package de.velcommuta.denul.data;

import de.velcommuta.denul.crypto.KeyExchange;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Data holder class for study requests
 */
public class StudyRequest {
    // Static constants
    public static final int VERIFY_FILE = 0;
    public static final String VERIFY_FILE_TITLE = "(easy) File";
    public static final String VERIFY_FILE_DESC_SHORT = "You will have to put a file into a specific location relative to your study URL";
    public static final String VERIFY_FILE_DESC_LONG = "To authenticate your request, you will have to put the following string into its own line in the file at %s:\n%s";
    public static final int VERIFY_META = 1;
    public static final String VERIFY_META_TITLE = "(advanced) <meta>-Tag";
    public static final String VERIFY_META_DESC_SHORT = "You will have to add a special <meta> tag to the source code of your study website";
    public static final String VERIFY_META_DESC_LONG = "To authenticate your request, you will have to put the following <meta> tag into the <head> of the HTML document at %s:\n<meta name='study-key' content='%s'>";
    public static final int VERIFY_DNS = 2;
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
    public int verification;

    // Cryptographic material
    public PublicKey pubkey;
    public PrivateKey privkey;
    public KeyExchange exchange;

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
    }
}
