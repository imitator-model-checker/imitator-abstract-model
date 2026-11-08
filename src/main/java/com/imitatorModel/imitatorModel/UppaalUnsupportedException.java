package com.imitatorModel.imitatorModel;

import java.util.List;

/**
 * The model (or a property) uses a feature UPPAAL cannot express, typically a parameter.
 * {@link #getProblems()} lists every reason; use IMITATOR for such a model ({@link ImitatorModel#preferredBackend}).
 */
public class UppaalUnsupportedException extends UnsupportedOperationException {

    private final List<String> problems;

    public UppaalUnsupportedException(List<String> problems) {
        super("The model cannot be translated to UPPAAL (use IMITATOR):\n  - " + String.join("\n  - ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> getProblems() {
        return problems;
    }
}
