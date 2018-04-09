package net.kemitix.outputcapture;

import java.util.concurrent.CountDownLatch;

/**
 * CountDownLatch that, when an {@link InterruptedException} is thrown, wraps it in a {@link OutputCaptureException}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class SafeLatch extends CountDownLatch {

    /**
     * Constructs a {@code CountDownLatch} initialized with the given count.
     *
     * @param count the number of times {@link #countDown} must be invoked
     *              before threads can pass through {@link #await}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    SafeLatch(int count) {
        super(count);
    }

    @Override
    public void await() {
        try {
            super.await();
        } catch (InterruptedException e) {
            throw new OutputCaptureException(e);
        }
    }
}
