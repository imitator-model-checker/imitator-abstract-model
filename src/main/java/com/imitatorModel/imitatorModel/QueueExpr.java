package com.imitatorModel.imitatorModel;

/**
 * Right-hand side of a queue update, built by {@link ImiQueue}: the empty queue {@code queue()}, a copy of
 * another queue, or the side effect {@code queue_push(e, q)} (printed alone, without {@code q <- }).
 * <p>
 * In UPPAAL a queue is a struct {@code {data[IMI__QUEUE_CAP]; len}}: push and clear are generated functions,
 * a copy is a struct assignment.
 */
public final class QueueExpr implements UpdateTerm {

    static final String EMPTY = "queue()";

    enum Kind { EMPTY, COPY, PUSH }

    private final Kind kind;
    private final UpdateTerm element;   // PUSH only
    private final ImiQueue queue;       // COPY: the source; PUSH: the target
    private final String imitator;

    private QueueExpr(Kind kind, UpdateTerm element, ImiQueue queue, String imitator) {
        this.kind = kind;
        this.element = element;
        this.queue = queue;
        this.imitator = imitator;
    }

    static QueueExpr empty() {
        return new QueueExpr(Kind.EMPTY, null, null, EMPTY);
    }

    static QueueExpr copyOf(ImiQueue queue) {
        return new QueueExpr(Kind.COPY, null, queue, queue.toIMITATOR());
    }

    static QueueExpr push(UpdateTerm element, ImiQueue queue) {
        return new QueueExpr(Kind.PUSH, element, queue,
                "queue_push(" + element.toIMITATOR() + ", " + queue.toIMITATOR() + ")");
    }

    /** Pushed element (null unless this is a push). */
    UpdateTerm getElement() {
        return element;
    }

    @Override
    public boolean isSideEffect() {
        return kind == Kind.PUSH;
    }

    @Override
    public String toIMITATOR() {
        return imitator;
    }

    /**
     * Push: the whole statement {@code imi__rqueue_push(q, v)}; clear: {@code imi__rqueue_clear(q)} (also a whole
     * statement, see {@link #isUppaalStatement()}); copy: the source queue (struct assignment).
     */
    @Override
    public String toUppaal(UppaalContext ctx, VariableType target) {
        ImiQueue q = (ImiQueue) target;
        ImiQueue.ElementType type = q.getElementType();
        return switch (kind) {
            case PUSH -> {
                String value = type == ImiQueue.ElementType.BOOL
                        ? ((BooleanExpr) element).toUppaal(ctx)
                        : ((LinearExpr) element).toUppaal(ctx,
                              type == ImiQueue.ElementType.INT ? UppaalContext.Sort.INT : UppaalContext.Sort.RATIONAL);
                yield ctx.queueFunction(type, "push") + "(" + q.toUppaal(ctx) + ", " + value + ")";
            }
            case EMPTY -> ctx.queueFunction(type, "clear") + "(" + q.toUppaal(ctx) + ")";
            case COPY -> queue.toUppaal(ctx);
        };
    }

    /** True if {@link #toUppaal} gives a whole statement rather than the right-hand side of {@code q = ...}. */
    boolean isUppaalStatement() {
        return kind != Kind.COPY;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof QueueExpr other && imitator.equals(other.imitator) && kind == other.kind;
    }

    @Override
    public int hashCode() {
        return imitator.hashCode();
    }
}
