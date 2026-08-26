package io.latticedb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseLifecycleTest {
    @TempDir
    Path dir;

    private String dbPath() {
        return dir.resolve("test.db").toString();
    }

    @Test
    void versionIsExposed() {
        String version = Database.version();
        assertNotNull(version);
        assertFalse(version.isEmpty());
    }

    @Test
    void openCreateAndReopen() {
        try (Database db = Database.open(dbPath(), OpenOptions.defaults().create(true))) {
            assertTrue(db.isOpen());
            assertEquals(dbPath(), db.getPath());
            db.write(txn -> {
            txn.createNode(java.util.List.of("Thing"),
                    java.util.Map.of("name", "n1"));
            return null;
        });
        }
        try (Database db = Database.open(dbPath())) {
            long count = db.read(txn -> (long) txn.query(
                    "MATCH (n:Thing) RETURN count(n) AS c").rows().get(0).get("c"));
            assertEquals(1L, count);
        }
    }

    @Test
    void openMissingFileFailsWithoutCreate() {
        assertThrows(LatticeException.class, () -> Database.open(dbPath()));
    }

    @Test
    void closeIsIdempotentAndDetectsUseAfterClose() {
        Database db = Database.open(dbPath(), OpenOptions.defaults().create(true));
        assertTrue(db.isOpen());
        db.close();
        assertFalse(db.isOpen());
        db.close(); // no-op
        assertThrows(LatticeException.class, db::cacheClear);
    }

    @Test
    void readOnlyDatabaseRejectsWrites() {
        try (Database db = Database.open(dbPath(),
                OpenOptions.defaults().create(true).readOnly(false))) {
            db.write(txn -> null);
        }
        try (Database db = Database.open(dbPath(), OpenOptions.defaults().readOnly(true))) {
            LatticeException ex = assertThrows(LatticeException.class, db::beginWrite);
            assertEquals(ErrorCode.READ_ONLY, ex.getErrorCode());
        }
    }

    @Test
    void secondWriterTimesOut() {
        try (Database db = Database.open(dbPath(), OpenOptions.defaults().create(true));
             Transaction writer = db.beginWrite()) {
            LatticeException ex = assertThrows(LatticeException.class, db::beginWrite);
            assertEquals(ErrorCode.LOCK_TIMEOUT, ex.getErrorCode());
            // readers are fine alongside a writer
            try (Transaction reader = db.beginRead()) {
                assertTrue(reader.isReadOnly());
            }
        }
    }

    @Test
    void writeLambdaRollsBackOnException() {
        try (Database db = Database.open(dbPath(), OpenOptions.defaults().create(true))) {
            assertThrows(IllegalStateException.class, () -> db.write(txn -> {
                txn.createNode(java.util.List.of("Thing"));
                throw new IllegalStateException("boom");
            }));
            long count = db.read(txn -> (long) txn.query(
                    "MATCH (n:Thing) RETURN count(n) AS c").rows().get(0).get("c"));
            assertEquals(0L, count);
        }
    }

    @Test
    void optionsDefaultsMatchGoBinding() {
        OpenOptions opts = OpenOptions.defaults();
        assertEquals(OpenOptions.defaults(), opts);
        assertThrows(IllegalArgumentException.class,
                () -> opts.vectorDimensions(0));
        assertThrows(IllegalArgumentException.class,
                () -> opts.vectorDimensions(5000));
    }
}
