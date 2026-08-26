package io.latticedb;

/** Query diagnostic stage, mirroring {@code lattice_query_error_stage}. */
public enum QueryErrorStage {
    NONE(0),
    PARSE(1),
    SEMANTIC(2),
    PLAN(3),
    EXECUTION(4);

    private final int stage;

    QueryErrorStage(int stage) {
        this.stage = stage;
    }

    public int stage() {
        return stage;
    }

    static QueryErrorStage fromStage(int stage) {
        for (QueryErrorStage s : values()) {
            if (s.stage == stage) {
                return s;
            }
        }
        throw new IllegalArgumentException("unknown query error stage: " + stage);
    }
}
