package io.latticedb;

/** Options for full-text search methods. */
public final class FTSSearchOptions {
    private int limit = 10;
    private int maxDistance = 0;      /* 0 = engine default (2) */
    private int minTermLength = 0;    /* 0 = engine default (4) */

    private FTSSearchOptions() {
    }

    public static FTSSearchOptions defaults() {
        return new FTSSearchOptions();
    }

    /** Maximum number of results. Default 10. */
    public FTSSearchOptions limit(int limit) {
        this.limit = limit;
        return this;
    }

    /** Max Levenshtein edit distance for fuzzy search; 0 = default 2. */
    public FTSSearchOptions maxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    /** Min term length for fuzzy expansion; 0 = default 4. */
    public FTSSearchOptions minTermLength(int minTermLength) {
        this.minTermLength = minTermLength;
        return this;
    }

    int limit() { return limit; }
    int maxDistance() { return maxDistance; }
    int minTermLength() { return minTermLength; }
}
