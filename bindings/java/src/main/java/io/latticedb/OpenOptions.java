package io.latticedb;

import java.util.Objects;

/**
 * Options for opening a database, mirroring the Go binding's OpenOptions and
 * the native {@code lattice_open_options_v4} struct. All values have sane
 * defaults; use the builder to override them.
 */
public final class OpenOptions {
    private boolean create = false;
    private boolean readOnly = false;
    private int cacheSizeMB = 100;
    private int pageSize = 4096;
    private boolean enableVectors = false;
    private int vectorDimensions = 128;
    private boolean enableWal = true;
    private boolean enableAdjacencyCache = false;
    private boolean lock = true;

    private OpenOptions() {
    }

    public static OpenOptions defaults() {
        return new OpenOptions();
    }

    public OpenOptions create(boolean create) {
        this.create = create;
        return this;
    }

    public OpenOptions readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public OpenOptions cacheSizeMB(int cacheSizeMB) {
        this.cacheSizeMB = cacheSizeMB;
        return this;
    }

    public OpenOptions pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /** Enable vector storage for embeddings. */
    public OpenOptions enableVectors(boolean enableVectors) {
        this.enableVectors = enableVectors;
        return this;
    }

    /** Vector dimensions, 1..4096. Default 128. */
    public OpenOptions vectorDimensions(int vectorDimensions) {
        if (vectorDimensions < 1 || vectorDimensions > 4096) {
            throw new IllegalArgumentException("vectorDimensions must be in 1..4096");
        }
        this.vectorDimensions = vectorDimensions;
        return this;
    }

    /** Enable WAL-backed transactions. Default true. */
    public OpenOptions enableWal(boolean enableWal) {
        this.enableWal = enableWal;
        return this;
    }

    /** Enable the in-memory graph adjacency cache. Default false. */
    public OpenOptions enableAdjacencyCache(boolean enableAdjacencyCache) {
        this.enableAdjacencyCache = enableAdjacencyCache;
        return this;
    }

    /**
     * Take a lock on the database file. Default true.
     *
     * <p>Turn this off only for filesystems where locking does not work; it
     * does not make concurrent access safe.</p>
     */
    public OpenOptions lock(boolean lock) {
        this.lock = lock;
        return this;
    }

    boolean create() { return create; }
    boolean readOnly() { return readOnly; }
    int cacheSizeMB() { return cacheSizeMB; }
    int pageSize() { return pageSize; }
    boolean enableVectors() { return enableVectors; }
    int vectorDimensions() { return vectorDimensions; }
    boolean enableWal() { return enableWal; }
    boolean enableAdjacencyCache() { return enableAdjacencyCache; }
    boolean lock() { return lock; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OpenOptions other)) return false;
        return create == other.create && readOnly == other.readOnly
                && cacheSizeMB == other.cacheSizeMB && pageSize == other.pageSize
                && enableVectors == other.enableVectors
                && vectorDimensions == other.vectorDimensions
                && enableWal == other.enableWal
                && enableAdjacencyCache == other.enableAdjacencyCache
                && lock == other.lock;
    }

    @Override
    public int hashCode() {
        return Objects.hash(create, readOnly, cacheSizeMB, pageSize, enableVectors,
                vectorDimensions, enableWal, enableAdjacencyCache, lock);
    }
}
