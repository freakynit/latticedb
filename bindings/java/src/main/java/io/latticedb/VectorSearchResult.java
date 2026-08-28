package io.latticedb;

/** One vector-search hit. */
public record VectorSearchResult(long nodeId, float distance) {
}
