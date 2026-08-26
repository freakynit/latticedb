package io.latticedb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StreamTest {
    @TempDir
    Path dir;

    @Test
    void publishReadOffsetTrim() {
        try (Database db = Database.open(dir.resolve("s.db").toString(),
                OpenOptions.defaults().create(true))) {
            long last = db.write(tx -> {
                tx.publishStream("events", null, Map.of("i", 1));
                tx.publishStream("events", "custom", List.of("a", "b"));
                long seq = tx.publishStreamGetSequence("events", null, Map.of("i", 3));
                return seq;
            });
            assertEquals(3L, last);

            List<StreamRecord> records = db.readStream("events", 0, 10, 0);
            assertEquals(3, records.size());
            assertEquals(1L, records.get(0).sequence());
            assertEquals("message", records.get(0).kind());
            assertEquals(Map.of("i", 1L), records.get(0).payload());
            assertEquals("custom", records.get(1).kind());

            // cursor semantics: read after sequence 1 yields the remainder
            List<StreamRecord> tail = db.readStream("events", 1, 10, 0);
            assertEquals(2, tail.size());

            assertTrue(db.getStreamOffset("events", "c1").isEmpty());
            db.write(tx -> {
                tx.setStreamOffset("events", "c1", records.get(records.size() - 1).sequence());
                return null;
            });
            assertEquals(3L, db.getStreamOffset("events", "c1").orElseThrow());

            assertEquals(3L, db.getLastSequence("events"));

            db.write(tx -> {
                tx.trimStream("events", 2);
                return null;
            });
            List<StreamRecord> remaining = db.readStream("events", 0, 10, 0);
            assertEquals(1, remaining.size());
            assertEquals(3L, remaining.get(0).sequence());
        }
    }

    @Test
    void changefeedReflectsGraphWrites() {
        try (Database db = Database.open(dir.resolve("c.db").toString(),
                OpenOptions.defaults().create(true))) {
            db.write(tx -> {
                Node a = tx.createNode(List.of("P"), Map.of("name", "Alice"));
                Node b = tx.createNode(List.of("P"));
                tx.createEdge(a.id(), b.id(), "KNOWS");
                return null;
            });

            List<StreamRecord> changes = db.changes(0, 100, 0);
            assertFalse(changes.isEmpty());
            assertTrue(changes.stream().allMatch(r -> r.kind() != null));
        }
    }

    @Test
    void reservedPrefixRejected() {
        try (Database db = Database.open(dir.resolve("r.db").toString(),
                OpenOptions.defaults().create(true))) {
            LatticeException ex = assertThrows(LatticeException.class,
                    () -> db.write(tx -> {
                        tx.publishStream("__lattice_forbidden", null, "x");
                        return null;
                    }));
            assertEquals(ErrorCode.INVALID_ARG, ex.getErrorCode());
        }
    }
}
