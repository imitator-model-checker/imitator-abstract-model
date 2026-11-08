package com.imitatorModel.imitatorModel;

/**
 * Options of the UPPAAL translation ({@link ImitatorModel#toUppaal}).
 *
 * @param intMin        lower bound of every UPPAAL {@code int} that stands for an IMITATOR {@code int} or {@code rational}
 * @param intMax        upper bound of the same; UPPAAL rejects a clock constraint whose bound can reach 2^30
 *                      ({@code Out of range clock constraint}), so keep {@code |bound| + constants < 2^30}
 * @param queueCapacity size of the array that stands for an IMITATOR queue (UPPAAL has no unbounded structure;
 *                      a push on a full queue aborts the verification with an out-of-range index)
 * @param timeScale     factor applied to every time-like value (clocks, rationals); 0 = smallest factor that makes
 *                      every constant an integer (lcm of the denominators)
 * @param layout        give the locations coordinates, so that the UPPAAL GUI does not stack them at the origin
 */
public record UppaalOptions(long intMin, long intMax, int queueCapacity, long timeScale, boolean layout) {

    public static final long DEFAULT_INT_BOUND = 100_000_000L;

    public UppaalOptions {
        if (intMin > intMax) {
            throw new IllegalArgumentException("intMin > intMax");
        }
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("queueCapacity must be positive");
        }
        if (timeScale < 0) {
            throw new IllegalArgumentException("timeScale must be positive (0 = automatic)");
        }
    }

    public static UppaalOptions defaults() {
        return new UppaalOptions(-DEFAULT_INT_BOUND, DEFAULT_INT_BOUND, 32, 0, true);
    }

    public UppaalOptions withIntRange(long min, long max) {
        return new UppaalOptions(min, max, queueCapacity, timeScale, layout);
    }

    public UppaalOptions withQueueCapacity(int capacity) {
        return new UppaalOptions(intMin, intMax, capacity, timeScale, layout);
    }

    public UppaalOptions withTimeScale(long scale) {
        return new UppaalOptions(intMin, intMax, queueCapacity, scale, layout);
    }

    public UppaalOptions withLayout(boolean layout) {
        return new UppaalOptions(intMin, intMax, queueCapacity, timeScale, layout);
    }
}
