package de.velcommuta.denul.database;

/**
 * Constract class holding the constants for the SQLite Database
 */
public class SQLContract {
    public static final String COMMA_SEP = ", ";
    public static class Studies {
        public static final String TABLE_NAME = "Studies";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_INSTITUTION = "institution";
        public static final String COLUMN_WEB = "webpage";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_PURPOSE = "purpose";
        public static final String COLUMN_PROCEDURES = "procedures";
        public static final String COLUMN_RISKS = "risks";
        public static final String COLUMN_BENEFITS = "benefits";
        public static final String COLUMN_PAYMENT = "payment";
        public static final String COLUMN_CONFLICTS = "conflicts";
        public static final String COLUMN_CONFIDENTIALITY = "confidentiality";
        public static final String COLUMN_PARTICIPATION = "participationAndWithdrawal";
        public static final String COLUMN_RIGHTS = "rights";
        public static final String COLUMN_VERIFICATION = "verification";
        public static final String COLUMN_PRIVKEY = "privkey";
        public static final String COLUMN_PUBKEY = "pubkey";
        public static final String COLUMN_KEYALGO = "keyalgo";
        public static final String COLUMN_KEX = "kex";
        public static final String COLUMN_KEXALGO = "kexalgo";
        public static final String COLUMN_QUEUE = "queue";



        // Constants
        public static final String CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_INSTITUTION + " TEXT, " +
                COLUMN_WEB + " TEXT, " +
                COLUMN_DESCRIPTION + " TEXT, " +
                COLUMN_PURPOSE + " TEXT, " +
                COLUMN_PROCEDURES + " TEXT, " +
                COLUMN_RISKS + " TEXT, " +
                COLUMN_BENEFITS + " TEXT, " +
                COLUMN_PAYMENT + " TEXT, " +
                COLUMN_CONFLICTS + " TEXT, " +
                COLUMN_CONFIDENTIALITY + " TEXT, " +
                COLUMN_PARTICIPATION + " TEXT, " +
                COLUMN_RIGHTS + " TEXT, " +
                COLUMN_VERIFICATION + " INT, " +
                COLUMN_PRIVKEY + " STRING, " +
                COLUMN_PUBKEY + " STRING, " +
                COLUMN_KEYALGO + " INTEGER, " +
                COLUMN_KEX + " BLOB, " +
                COLUMN_KEXALGO + " INTEGER, " +
                COLUMN_QUEUE + " BLOB" +
                ");";

        public static final String INSERT = "INSERT INTO " + TABLE_NAME + "(" + COLUMN_NAME + COMMA_SEP +
                COLUMN_INSTITUTION + COMMA_SEP + COLUMN_WEB + COMMA_SEP + COLUMN_DESCRIPTION + COMMA_SEP +
                COLUMN_PURPOSE + COMMA_SEP + COLUMN_PROCEDURES + COMMA_SEP + COLUMN_RISKS + COMMA_SEP +
                COLUMN_BENEFITS + COMMA_SEP + COLUMN_PAYMENT + COMMA_SEP + COLUMN_CONFLICTS + COMMA_SEP +
                COLUMN_CONFIDENTIALITY + COMMA_SEP + COLUMN_PARTICIPATION + COMMA_SEP + COLUMN_RIGHTS + COMMA_SEP +
                COLUMN_VERIFICATION + COMMA_SEP + COLUMN_PRIVKEY + COMMA_SEP + COLUMN_PUBKEY + COMMA_SEP +
                COLUMN_KEYALGO + COMMA_SEP + COLUMN_KEX + COMMA_SEP + COLUMN_KEXALGO + COMMA_SEP + COLUMN_QUEUE +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        public static final String DROP = "DROP TABLE " + TABLE_NAME + ";";

        public static final String SELECT_BY_ID = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " LIKE ?;";
        public static final String SELECT_ALL = "SELECT * FROM " + TABLE_NAME + ";";
        public static final String SELECT_BY_QUEUE = "SELECT " + COLUMN_ID + " FROM " + TABLE_NAME + " WHERE " +
                COLUMN_QUEUE + " LIKE ?;";
    }

    public static class Investigators {
        public static final String TABLE_NAME = "Investigators";

        public static final String COLUMN_ID = "id";
        public static final String COLUMN_STUDY = "study";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_INSTITUTION = "institution";
        public static final String COLUMN_GROUP = "wg"; // Working group, as GROUP is a keyword in SQL
        public static final String COLUMN_POSITION = "pos";

        public static final String CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_STUDY + " INTEGER NOT NULL, " +
                COLUMN_NAME + " TEXT NOT NULL, " +
                COLUMN_INSTITUTION + " TEXT NOT NULL, " +
                COLUMN_GROUP + " TEXT NOT NULL, " +
                COLUMN_POSITION + " TEXT NOT NULL, " +
                "FOREIGN KEY (" + COLUMN_STUDY + ") REFERENCES " + Studies.TABLE_NAME + "(" + Studies.COLUMN_ID + ") " +
                "ON DELETE CASCADE);";

        public static final String INSERT = "INSERT INTO " + TABLE_NAME + "(" + COLUMN_STUDY + COMMA_SEP +
                COLUMN_NAME + COMMA_SEP + COLUMN_INSTITUTION + COMMA_SEP + COLUMN_GROUP + COMMA_SEP +
                COLUMN_POSITION + ") VALUES (?, ?, ?, ?, ?);";

        public static final String SELECT_STUDY_ID = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_STUDY + " LIKE ?;";
    }

    public static class DataRequests {
        public static final String TABLE_NAME = "DataRequests";

        public static final String COLUMN_ID = "id";
        public static final String COLUMN_STUDY = "study";
        public static final String COLUMN_DATATYPE = "datatype";
        public static final String COLUMN_GRANULARITY = "granularity";
        public static final String COLUMN_FREQUENCY = "frequency";

        public static final String CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_STUDY + " INTEGER NOT NULL, " +
                COLUMN_DATATYPE + " INTEGER NOT NULL, " +
                COLUMN_GRANULARITY + " INTEGER NOT NULL, " +
                COLUMN_FREQUENCY + " INTEGER NOT NULL, " +
                "FOREIGN KEY (" + COLUMN_STUDY + ") REFERENCES " + Studies.TABLE_NAME + "(" + Studies.COLUMN_ID + ")" +
                " ON DELETE CASCADE);";
        public static final String INSERT = "INSERT INTO " + TABLE_NAME + "(" + COLUMN_STUDY + COMMA_SEP +
                COLUMN_DATATYPE + COMMA_SEP + COLUMN_GRANULARITY + COMMA_SEP + COLUMN_FREQUENCY +
                ") VALUES (?, ?, ?, ?);";

        public static final String SELECT_STUDY_ID = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_STUDY + " LIKE ?;";
    }

    public static class StudyParticipants {
        public static final String TABLE_NAME = "StudyParticipants";

        public static final String COLUMN_ID = "id";
        // Which study is this participant associated with? (Foreign key)
        public static final String COLUMN_STUDY = "study";
        // Key to use when encrypting FOR this person
        public static final String COLUMN_KEY_OUT = "key_out";
        // Counter to use when encrypting FOR this person
        public static final String COLUMN_CTR_OUT = "ctr_out";
        // Key to use when decrypting FROM this person
        public static final String COLUMN_KEY_IN = "key_in";
        // Counter to use when decrypting FROM this person
        public static final String COLUMN_CTR_IN = "ctr_in";

        public static final String CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_STUDY + " INTEGER NOT NULL, " +
                COLUMN_KEY_OUT + " BLOB NOT NULL, " +
                COLUMN_CTR_OUT + " BLOB NOT NULL, " +
                COLUMN_KEY_IN + " BLOB NOT NULL, " +
                COLUMN_CTR_IN + " BLOB NOT NULL, " +
                "FOREIGN KEY (" + COLUMN_STUDY + ") REFERENCES " + Studies.TABLE_NAME + "(" + Studies.COLUMN_ID + ") " +
                "ON DELETE CASCADE);";

        public static final String INSERT = "INSERT INTO " + TABLE_NAME + " (" + COLUMN_STUDY + COMMA_SEP +
                COLUMN_KEY_OUT + COMMA_SEP + COLUMN_CTR_OUT + COMMA_SEP + COLUMN_KEY_IN + COMMA_SEP + COLUMN_CTR_IN +
                ") VALUES (?,?,?,?,?);";

        public static final String SELECT_PARTICIPANT_ID = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ID +
                " LIKE ?;";
        public static final String SELECT_PARTICIPANT_STUDY = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_STUDY +
                " LIKE ?;";
    }

}
