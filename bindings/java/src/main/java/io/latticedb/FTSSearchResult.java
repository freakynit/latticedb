package io.latticedb;

/** One full-text search hit (BM25-scored). */
public record FTSSearchResult(long nodeId, float score) {
}
