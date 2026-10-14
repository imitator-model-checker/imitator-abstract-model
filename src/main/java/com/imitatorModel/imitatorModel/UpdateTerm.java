package com.imitatorModel.imitatorModel;

public interface UpdateTerm {
    String toIMITATOR();

    /** True for a statement such as {@code queue_push(e, q)}, printed alone instead of {@code target <- term}. */
    default boolean isSideEffect() {
        return false;
    }
}
