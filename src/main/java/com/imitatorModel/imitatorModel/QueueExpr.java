package com.imitatorModel.imitatorModel;

/**
 * Right-hand side of a queue update, built by {@link ImiQueue}: the empty queue {@code queue()}, a copy of
 * another queue, or the side effect {@code queue_push(e, q)} (printed alone, without {@code q <- }).
 */
public final class QueueExpr implements UpdateTerm {

    static final String EMPTY = "queue()";

    private final String imitator;
    private final boolean sideEffect;

    private QueueExpr(String imitator, boolean sideEffect) {
        this.imitator = imitator;
        this.sideEffect = sideEffect;
    }

    static QueueExpr empty() {
        return new QueueExpr(EMPTY, false);
    }

    static QueueExpr copyOf(ImiQueue queue) {
        return new QueueExpr(queue.toIMITATOR(), false);
    }

    static QueueExpr push(UpdateTerm element, ImiQueue queue) {
        return new QueueExpr("queue_push(" + element.toIMITATOR() + ", " + queue.toIMITATOR() + ")", true);
    }

    @Override
    public boolean isSideEffect() {
        return sideEffect;
    }

    @Override
    public String toIMITATOR() {
        return imitator;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof QueueExpr other && imitator.equals(other.imitator) && sideEffect == other.sideEffect;
    }

    @Override
    public int hashCode() {
        return imitator.hashCode();
    }
}
