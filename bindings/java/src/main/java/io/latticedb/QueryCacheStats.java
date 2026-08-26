package io.latticedb;

/** Query cache statistics. */
public record QueryCacheStats(int entries, long hits, long misses) {
}
