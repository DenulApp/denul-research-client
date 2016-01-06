package de.velcommuta.denul.data;

/**
 * Data container for StudyJoinRequests
 */
public class StudyJoinRequest {
    public enum KEX_ALGO {
        KEX_UNKNOWN,
        KEX_ECDH_CURVE25519
    }

    public byte[] kexpub;
    public KEX_ALGO kexalgo;
    public byte[] queue;
}
