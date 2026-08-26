package io.latticedb;

/** Error codes mirroring {@code lattice_error} from the native C API. */
public enum ErrorCode {
    OK(0),
    ERROR(-1),
    IO(-2),
    CORRUPTION(-3),
    NOT_FOUND(-4),
    ALREADY_EXISTS(-5),
    INVALID_ARG(-6),
    TXN_ABORTED(-7),
    LOCK_TIMEOUT(-8),
    READ_ONLY(-9),
    FULL(-10),
    VERSION_MISMATCH(-11),
    CHECKSUM(-12),
    OUT_OF_MEMORY(-13),
    UNSUPPORTED(-14),
    VALUE_TOO_LARGE(-15);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    /** The native {@code lattice_error} value. */
    public int code() {
        return code;
    }

    static ErrorCode fromCode(int code) {
        for (ErrorCode ec : values()) {
            if (ec.code == code) {
                return ec;
            }
        }
        throw new IllegalArgumentException("unknown error code: " + code);
    }
}
