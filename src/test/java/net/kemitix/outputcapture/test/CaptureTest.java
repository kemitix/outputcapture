package net.kemitix.outputcapture.test;

import net.kemitix.outputcapture.*;
import org.junit.*;
import org.junit.rules.Timeout;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void canRestoreSystemOutAndErrWhenMultipleOutputCapturesOverlap() {
        //given
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final SafeLatch latch1 = new SafeLatch(1, 1000L);
        final SafeLatch latch2 = new SafeLatch(1, 1000L);
        //when
        final OngoingCapturedOutput output1 = CaptureOutput.ofThread(() -> waitUntilReleased(latch1), maxAwaitMilliseconds);
        final OngoingCapturedOutput output2 = CaptureOutput.ofThread(() -> waitUntilReleased(latch2), maxAwaitMilliseconds);
        latch1.countDown();
        latch2.countDown();
        output1.getCompletedLatch().await();
        output2.getCompletedLatch().await();
        //then
        assertThat(System.out).isSameAs(originalOut);
        assertThat(System.err).isSameAs(originalErr);
    }

    private void waitUntilReleased(final SafeLatch latch) {
        latch.await();
    }

}
