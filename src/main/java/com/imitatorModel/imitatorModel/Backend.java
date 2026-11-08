package com.imitatorModel.imitatorModel;

/** Model checker that receives the model: see {@link ImitatorModel#preferredBackend(Property...)}. */
public enum Backend {
    /** {@code .imi} + {@code .imiprop}: needed for parameters (synthesis) and the features UPPAAL lacks. */
    IMITATOR,
    /** {@code .xml} (+ {@code .q}): non-parametric models only. */
    UPPAAL
}
