package io.latticedb;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A graph node returned by transaction operations.
 *
 * @param id         stable node id
 * @param labels     labels attached to the node
 * @param properties property map
 */
public record Node(long id, List<String> labels, Map<String, Object> properties) {
    public Node {
        labels = labels == null ? List.of() : Collections.unmodifiableList(labels);
        properties = properties == null ? Map.of() : Collections.unmodifiableMap(properties);
    }
}
