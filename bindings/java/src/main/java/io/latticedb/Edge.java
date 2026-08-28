package io.latticedb;

import java.util.Collections;
import java.util.Map;

/**
 * A graph edge returned by transaction operations.
 *
 * @param id         stable edge id
 * @param sourceId   source node id
 * @param targetId   target node id
 * @param type       edge type
 * @param properties property map
 */
public record Edge(long id, long sourceId, long targetId, String type,
                   Map<String, Object> properties) {
    public Edge {
        properties = properties == null ? Map.of() : Collections.unmodifiableMap(properties);
    }
}
