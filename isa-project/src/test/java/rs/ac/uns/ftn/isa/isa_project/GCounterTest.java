package rs.ac.uns.ftn.isa.isa_project;
import org.junit.jupiter.api.Test;
import rs.ac.uns.ftn.isa.isa_project.crdt.GCounter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
class GCounterTest {

    @Test
    void testIncrement() {
        GCounter counter = new GCounter();

        counter.increment("replica-1");
        assertEquals(1, counter.getValue());

        counter.increment("replica-1");
        assertEquals(2, counter.getValue());

        counter.increment("replica-2");
        assertEquals(3, counter.getValue());
    }

    @Test
    void testIncrementByAmount() {
        GCounter counter = new GCounter();

        counter.increment("replica-1", 10);
        assertEquals(10, counter.getValue());

        counter.increment("replica-1", 5);
        assertEquals(15, counter.getValue());
    }

    @Test
    void testGetValue() {
        GCounter counter = new GCounter();

        // Prazna mapa
        assertEquals(0, counter.getValue());

        // Dodaj različite replike
        counter.increment("replica-1", 30);
        counter.increment("replica-2", 20);
        counter.increment("replica-3", 50);

        // Suma: 30 + 20 + 50 = 100
        assertEquals(100, counter.getValue());
    }

    @Test
    void testGetReplicaCount() {
        GCounter counter = new GCounter();

        counter.increment("replica-1", 10);
        assertEquals(10, counter.getReplicaCount("replica-1"));

        counter.increment("replica-2", 5);
        assertEquals(5, counter.getReplicaCount("replica-2"));

        // Nepostojeća replika
        assertEquals(0, counter.getReplicaCount("replica-999"));
    }

    @Test
    void testMerge_NoConflict() {
        // Replika 1 ima samo svoje preglede
        GCounter counter1 = new GCounter();
        counter1.increment("replica-1", 30);

        // Replika 2 ima samo svoje preglede
        GCounter counter2 = new GCounter();
        counter2.increment("replica-2", 20);

        // Merge
        GCounter merged = counter1.merge(counter2);

        // Rezultat: {replica-1: 30, replica-2: 20}
        assertEquals(50, merged.getValue());
        assertEquals(30, merged.getReplicaCount("replica-1"));
        assertEquals(20, merged.getReplicaCount("replica-2"));
    }

    @Test
    void testMerge_WithConflict() {
        // Replika 1: {replica-1: 30, replica-2: 10}
        GCounter counter1 = new GCounter();
        counter1.increment("replica-1", 30);
        counter1.increment("replica-2", 10);

        // Replika 2: {replica-1: 20, replica-2: 25}
        GCounter counter2 = new GCounter();
        counter2.increment("replica-1", 20);
        counter2.increment("replica-2", 25);

        // Merge - uzima MAX za svaku repliku
        GCounter merged = counter1.merge(counter2);

        // Rezultat: {replica-1: 30 (MAX), replica-2: 25 (MAX)}
        assertEquals(55, merged.getValue()); // 30 + 25
        assertEquals(30, merged.getReplicaCount("replica-1")); // MAX(30, 20)
        assertEquals(25, merged.getReplicaCount("replica-2")); // MAX(10, 25)
    }

    @Test
    void testMerge_Idempotence() {
        GCounter counter = new GCounter();
        counter.increment("replica-1", 50);

        // Merge sa samim sobom
        GCounter merged = counter.merge(counter);

        // Rezultat mora biti isti
        assertEquals(50, merged.getValue());
    }

    @Test
    void testMerge_Commutativity() {
        GCounter counter1 = new GCounter();
        counter1.increment("replica-1", 30);
        counter1.increment("replica-2", 10);

        GCounter counter2 = new GCounter();
        counter2.increment("replica-1", 20);
        counter2.increment("replica-2", 25);

        // merge(A, B) == merge(B, A)
        GCounter mergedAB = counter1.merge(counter2);
        GCounter mergedBA = counter2.merge(counter1);

        assertEquals(mergedAB.getValue(), mergedBA.getValue());
        assertEquals(mergedAB.getCounts(), mergedBA.getCounts());
    }

    @Test
    void testNegativeIncrementThrowsException() {
        GCounter counter = new GCounter();

        assertThrows(IllegalArgumentException.class, () -> {
            counter.increment("replica-1", -5);
        });
    }

    @Test
    void testGetCounts() {
        GCounter counter = new GCounter();
        counter.increment("replica-1", 10);
        counter.increment("replica-2", 20);

        Map<String, Long> counts = counter.getCounts();

        assertEquals(2, counts.size());
        assertEquals(10L, counts.get("replica-1"));
        assertEquals(20L, counts.get("replica-2"));

        // Provera da je vraćena kopija (ne može se menjati original)
        counts.put("replica-3", 999L);
        assertEquals(0, counter.getReplicaCount("replica-3"));
    }

    @Test
    void testToString() {
        GCounter counter = new GCounter();
        counter.increment("replica-1", 50);

        String str = counter.toString();
        assertTrue(str.contains("replica-1"));
        assertTrue(str.contains("50"));
        assertTrue(str.contains("total=50"));
    }
}
