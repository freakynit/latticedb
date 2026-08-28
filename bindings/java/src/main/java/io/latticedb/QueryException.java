package io.latticedb;

import java.util.Optional;

/**
 * Exception thrown when a Cypher query fails to prepare or execute.
 *
 * Carries structured diagnostics: the pipeline stage that failed, an optional
 * engine diagnostic code, and an optional source location.
 */
public class QueryException extends RuntimeException {
    private final ErrorCode code;
    private final QueryErrorStage stage;
    private final String diagnosticCode;
    private final boolean hasLocation;
    private final int line;
    private final int column;
    private final int length;

    QueryException(int code, int stage, String message, String diagnosticCode,
                   boolean hasLocation, int line, int column, int length) {
        super(message);
        this.code = ErrorCode.fromCode(code);
        this.stage = QueryErrorStage.fromStage(stage);
        this.diagnosticCode = diagnosticCode;
        this.hasLocation = hasLocation;
        this.line = line;
        this.column = column;
        this.length = length;
    }

    public ErrorCode getCode() {
        return code;
    }

    /** The pipeline stage that failed (parse, semantic, plan, execution). */
    public QueryErrorStage getStage() {
        return stage;
    }

    /** Engine diagnostic code, if any (e.g. {@code unknown_label}). */
    public Optional<String> getDiagnosticCode() {
        return Optional.ofNullable(diagnosticCode);
    }

    public boolean hasLocation() {
        return hasLocation;
    }

    /** 1-based line of the failing token, or 0 when unavailable. */
    public int getLine() {
        return line;
    }

    /** 1-based column of the failing token, or 0 when unavailable. */
    public int getColumn() {
        return column;
    }

    /** Length of the failing token span, or 0 when unavailable. */
    public int getLength() {
        return length;
    }

    @Override
    public String getMessage() {
        String base = super.getMessage();
        return diagnosticCode != null ? base + " (" + diagnosticCode + ")" : base;
    }
}
