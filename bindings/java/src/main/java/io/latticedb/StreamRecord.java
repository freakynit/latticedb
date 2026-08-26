package io.latticedb;

/** One record read from a durable stream. */
public record StreamRecord(long sequence, String kind, Object payload) {
}
