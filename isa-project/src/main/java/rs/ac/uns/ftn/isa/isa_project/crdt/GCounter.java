package rs.ac.uns.ftn.isa.isa_project.crdt;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * G-Counter (Grow-only Counter) - CRDT struktura za brojač koji samo raste.
 *
 * Svaka replika čuva svoj lokalni brojač u mapi.
 * Ukupna vrednost je suma svih replika.
 *
 * Primer:
 * - Replika 1: {replica-1: 30, replica-2: 0}
 * - Replika 2: {replica-1: 0, replica-2: 20}
 * - Nakon merge: {replica-1: 30, replica-2: 20}
 * - Ukupno: 50
 */
public class GCounter {

    /**
     * Mapa koja čuva brojač za svaku repliku.
     * Key: ID replike (npr. "replica-1", "replica-2")
     * Value: Broj inkremenata te replike
     */
    private final Map<String, Long> counts;

    /**
     * Default konstruktor - kreira prazan G-Counter
     */
    public GCounter() {
        this.counts = new HashMap<>();
    }

    /**
     * Konstruktor koji prima postojeću mapu brojača
     * Koristi se prilikom deserijalizacije ili testiranja
     */
    public GCounter(Map<String, Long> counts) {
        this.counts = new HashMap<>(counts);
    }

    /**
     * Inkrementuje brojač za datu repliku.
     *
     * @param replicaId ID replike (npr. "replica-1")
     */
    public void increment(String replicaId) {
        counts.put(replicaId, counts.getOrDefault(replicaId, 0L) + 1);
    }

    /**
     * Inkrementuje brojač za datu repliku za određenu vrednost.
     *
     * @param replicaId ID replike
     * @param amount Koliko da se doda
     */
    public void increment(String replicaId, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("G-Counter can only grow, amount must be >= 0");
        }
        counts.put(replicaId, counts.getOrDefault(replicaId, 0L) + amount);
    }

    /**
     * Vraća ukupnu vrednost brojača - SUMA svih replika.
     *
     * @return Ukupan broj pregleda
     */
    public long getValue() {
        return counts.values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    /**
     * Vraća vrednost brojača za određenu repliku.
     *
     * @param replicaId ID replike
     * @return Broj pregleda te replike
     */
    public long getReplicaCount(String replicaId) {
        return counts.getOrDefault(replicaId, 0L);
    }

    /**
     * Vraća kopiju interne mape brojača.
     * Koristi se za serijalizaciju i slanje preko mreže.
     *
     * @return Kopija mape brojača
     */
    public Map<String, Long> getCounts() {
        return new HashMap<>(counts);
    }

    /**
     * MERGE funkcija - spaja dva G-Counter-a uzimajući MAX za svaku repliku.
     *
     * Ovo je ključna operacija za CRDT:
     * - Uzima MAX vrednost za svaku repliku
     * - Garantuje eventual consistency
     * - Komutativna je: merge(A, B) = merge(B, A)
     * - Idempotentna je: merge(A, A) = A
     *
     * Primer:
     * Counter1: {replica-1: 30, replica-2: 10}
     * Counter2: {replica-1: 20, replica-2: 25}
     * Merge:    {replica-1: 30, replica-2: 25}
     *
     * @param other Drugi G-Counter za merge
     * @return Novi G-Counter sa merged vrednostima
     */
    public GCounter merge(GCounter other) {
        Map<String, Long> merged = new HashMap<>(this.counts);

        // Za svaku repliku iz drugog counter-a
        for (Map.Entry<String, Long> entry : other.counts.entrySet()) {
            String replicaId = entry.getKey();
            Long otherCount = entry.getValue();

            // Uzmi MAX između lokalne i remote vrednosti
            merged.put(replicaId, Math.max(
                    merged.getOrDefault(replicaId, 0L),
                    otherCount
            ));
        }

        return new GCounter(merged);
    }

    /**
     * Vraća broj replika koje su zabeležile preglede
     */
    public int getReplicaCount() {
        return counts.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GCounter gCounter = (GCounter) o;
        return Objects.equals(counts, gCounter.counts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(counts);
    }

    @Override
    public String toString() {
        return "GCounter{" +
                "counts=" + counts +
                ", total=" + getValue() +
                '}';
    }
}