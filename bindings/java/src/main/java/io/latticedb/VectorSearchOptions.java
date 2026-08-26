package io.latticedb;

/** Options for {@code Database#vectorSearch}. */
public final class VectorSearchOptions {
    private int k = 10;
    private int efSearch = 0; /* 0 = engine default */

    private VectorSearchOptions() {
    }

    public static VectorSearchOptions defaults() {
        return new VectorSearchOptions();
    }

    /** Number of results to return. Default 10. */
    public VectorSearchOptions k(int k) {
        this.k = k;
        return this;
    }

    /** HNSW ef parameter; 0 uses the engine default. */
    public VectorSearchOptions efSearch(int efSearch) {
        this.efSearch = efSearch;
        return this;
    }

    int k() { return k; }
    int efSearch() { return efSearch; }
}
