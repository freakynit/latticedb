package io.latticedb;

import java.util.List;
import java.util.Map;

/**
 * Result of a Cypher query: column names plus one map per row keyed by
 * column name.
 */
public record QueryResult(List<String> columns, List<Map<String, Object>> rows) {
    public QueryResult {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
