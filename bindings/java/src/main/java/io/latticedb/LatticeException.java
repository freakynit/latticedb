package io.latticedb;

/**
 * Runtime exception carrying a native {@code lattice_error} code.
 *
 * Thrown by all LatticeDB operations that fail at the native layer.
 */
public class LatticeException extends RuntimeException {
    private final ErrorCode errorCode;

    public LatticeException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    LatticeException(int rawCode, String message) {
        this(ErrorCode.fromCode(rawCode), message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
