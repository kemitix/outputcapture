package net.kemitix.outputcapture.test;

import net.kemitix.outputcapture.CaptureOutput;
import net.kemitix.outputcapture.SafeLatch;
import net.kemitix.outputcapture.ThrowingCallable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;

import java.io.PrintStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractCaptureTest {

    //<editor-fold desc="fields">
    public static final String STARTING_OUT = "starting out";
    public static final String STARTING_ERR = "starting err";
    public static final String FINISHED_OUT = "finished out";
    public static final String FINISHED_ERR = "finished err";

    public final Long maxAwaitMilliseconds = 100L;
    public final String line1 = "1:" + UUID.randomUUID().toString();
    public final String line2 = "2:" + UUID.randomUUID().toString();
    //</editor-fold>

    @Before
    public void setUp() {
        System.out.println("AbstractCaptureTest.setUp");
        assertThat(CaptureOutput.activeCount()).as("No existing captures").isZero();
    }

    @After
    public void tearDown() {
        System.out.println("AbstractCaptureTest.tearDown");
        final int activeCount = CaptureOutput.activeCount();
        CaptureOutput.removeAllInterceptors();
        assertThat(activeCount).as("All captures removed").isZero();
    }

    protected LatchPair whenReleased(final Runnable callable) {
        final LatchPair latchPair = new LatchPair();
        new Thread(() -> {
            awaitLatch(latchPair.ready);
            callable.run();
            releaseLatch(latchPair.done);
        }).start();
        return latchPair;
    }

    protected void writeOutput(final PrintStream out, final String... lines) {
        for (String line : lines) {
            out.println(line);
        }
    }

    protected void awaitLatch(final SafeLatch latch) {
        latch.await();
    }

    protected void releaseLatch(final SafeLatch latch) {
        latch.countDown();
    }

    protected SafeLatch createLatch() {
        return new SafeLatch(1, 1000L);
    }

    protected void asyncWithInterrupt(final SafeLatch ready, final SafeLatch done) {
        System.out.println(STARTING_OUT);
        System.err.println(STARTING_ERR);
        releaseLatch(ready);
        awaitLatch(done);
        System.out.println(FINISHED_OUT);
        System.err.println(FINISHED_ERR);
    }

    protected ThrowingCallable waitAndContinue(final SafeLatch ready, final SafeLatch done) {
        return () -> {
            releaseLatch(ready);
            awaitLatch(done);
        };
    }

    protected class LatchPair {
        private final SafeLatch ready = new SafeLatch(1, 1000L);
        private final SafeLatch done = new SafeLatch(1, 1000L);

        void releaseAndWait() {
            ready.countDown();
            awaitLatch(done);
        }
    }
}
