package rs.ac.uns.ftn.isa.isa_project.model;

/**
 * Status transcoding posla.
 */
public enum TranscodingStatus {
    PENDING,        // Čeka u queue-u
    PROCESSING,     // U obradi
    COMPLETED,      // Uspešno završen
    FAILED,         // Neuspešan
    CANCELLED       // Otkazan
}