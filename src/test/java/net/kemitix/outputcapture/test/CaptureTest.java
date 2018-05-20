package net.kemitix.outputcapture.test;

import net.kemitix.outputcapture.*;
import org.junit.*;
import org.junit.rules.Timeout;

import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CaptureTest {

    private final Long maxAwaitMilliseconds = 100L;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(100L);

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

    private void awaitLatch(final SafeLatch latch) {
            latch.await();
    }

    @Test
    public void canRestoreSystemOutAndErrWhenMultipleOutputCapturesOverlap() {
        //given
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final SafeLatch latch1 = createLatch();
        final SafeLatch latch2 = createLatch();
        //when
        CaptureOutput.of(() ->
                CaptureOutput.of(
                        waitAndContinue(latch1, latch2),
                        maxAwaitMilliseconds),
                maxAwaitMilliseconds);
        releaseLatch(latch1);
        awaitLatch(latch2);
        //then
        assertThat(System.out).isSameAs(originalOut);
        assertThat(System.err).isSameAs(originalErr);
    }

    private void releaseLatch(final SafeLatch latch) {
        latch.countDown();
    }

    private SafeLatch createLatch() {
        return new SafeLatch(1, 1000L);
    }

    private ThrowingCallable waitAndContinue(final SafeLatch ready, final SafeLatch done) {
        return () -> {
            releaseLatch(ready);
            awaitLatch(done);
        };
    }

}
