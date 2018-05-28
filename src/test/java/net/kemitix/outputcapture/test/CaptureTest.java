package net.kemitix.outputcapture.test;

import net.kemitix.outputcapture.CaptureOutput;
import net.kemitix.outputcapture.OngoingCapturedOutput;
import net.kemitix.outputcapture.SafeLatch;
import net.kemitix.outputcapture.ThrowingCallable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CaptureTest {

    private final Long maxAwaitMilliseconds = 100L;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(maxAwaitMilliseconds);

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
        final SafeLatch latch1 = new SafeLatch(1, maxAwaitMilliseconds, () -> {
        });
        final SafeLatch latch2 = new SafeLatch(1, maxAwaitMilliseconds, () -> {
        });
        //when
        final OngoingCapturedOutput output1 = CaptureOutput.ofThread(() -> waitUntilReleased(latch1), maxAwaitMilliseconds);
        final OngoingCapturedOutput output2 = CaptureOutput.ofThread(() -> waitUntilReleased(latch2), maxAwaitMilliseconds);
        latch1.countDown();
        latch2.countDown();
        output1.join();
        output2.join();
        //then
        assertThat(System.out).isSameAs(originalOut);
        assertThat(System.err).isSameAs(originalErr);
    }

    @Test
    public void asyncDocTest() {
        //given
        final String line1 = "line 1";
        final String line2 = "line 2";
        final String line3 = "line 3";
        final String line4 = "line 4";
        ThrowingCallable runnable = () -> {
            System.out.println(line1);
            System.out.println(line2);
            System.out.println(line3);
            System.out.println(line4);
        };
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.ofThread(runnable, 1000L);
        //then
        // do other things
        ongoingCapturedOutput.join();
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(line1, line2, line3, line4);
    }

    private void waitUntilReleased(final SafeLatch latch) {
        latch.await();
    }

}
