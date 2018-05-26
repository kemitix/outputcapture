package net.kemitix.outputcapture.test;

import net.kemitix.outputcapture.CaptureOutput;
import net.kemitix.outputcapture.SafeLatch;
import org.junit.After;
import org.junit.Before;

import java.io.PrintStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractCaptureTest {

    //<editor-fold desc="fields">
    static final long MAX_TIMEOUT = 100L;
    static final String STARTING_OUT = "starting out";
    static final String STARTING_ERR = "starting err";
    static final String FINISHED_OUT = "finished out";
    static final String FINISHED_ERR = "finished err";

    final String line1 = "1:" + UUID.randomUUID().toString();
    final String line2 = "2:" + UUID.randomUUID().toString();
    //</editor-fold>

    @Before
    public void setUp() {
        assertThat(CaptureOutput.activeCount()).as("No existing captures").isZero();
    }

    @After
    public void tearDown() {
        final int activeCount = CaptureOutput.activeCount();
        CaptureOutput.removeAllInterceptors();
        assertThat(activeCount).as("All captures removed").isZero();
    }

    LatchPair whenReleased(final Runnable callable) {
        final LatchPair latchPair = new LatchPair();
        new Thread(() -> {
            awaitLatch(latchPair.ready);
            callable.run();
            releaseLatch(latchPair.done);
        }).start();
        return latchPair;
    }

    void writeOutput(final PrintStream out, final String... lines) {
        for (String line : lines) {
            out.println(line);
        }
    }

    void awaitLatch(final SafeLatch latch) {
        latch.await();
    }

    void releaseLatch(final SafeLatch latch) {
        latch.countDown();
    }

    SafeLatch createLatch() {
        return new SafeLatch(1, MAX_TIMEOUT, () -> {});
    }

    void asyncWithInterrupt(final SafeLatch ready, final SafeLatch done) {
        System.out.println(STARTING_OUT);
        System.err.println(STARTING_ERR);
        releaseLatch(ready);
        awaitLatch(done);
        System.out.println(FINISHED_OUT);
        System.err.println(FINISHED_ERR);
    }

    void doNothing() {
        // do nothing
    }

    class LatchPair {
        private final SafeLatch ready = createLatch();
        private final SafeLatch done = createLatch();

        void releaseAndWait() {
            ready.countDown();
            awaitLatch(done);
        }
    }
}
