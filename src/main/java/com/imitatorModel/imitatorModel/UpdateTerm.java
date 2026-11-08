package com.imitatorModel.imitatorModel;

public interface UpdateTerm {
    String toIMITATOR();

    /** UPPAAL right-hand side when assigned to {@code target} (or the whole statement for a side effect). */
    String toUppaal(UppaalContext ctx, VariableType target);

    /** True for a statement such as {@code queue_push(e, q)}, printed alone instead of {@code target <- term}. */
    default boolean isSideEffect() {
        return false;
    }
}
