package net.kemitix.outputcapture.test;

import net.kemitix.outputcapture.CaptureOutput;
import net.kemitix.outputcapture.CapturedOutput;
import net.kemitix.outputcapture.OngoingCapturedOutput;
import net.kemitix.outputcapture.SafeLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

// CaptureOutput.ofThread(...)
public class AsynchronousFilteredRedirect extends AbstractCaptureTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(MAX_TIMEOUT);

    private final AtomicReference<PrintStream> original = new AtomicReference<>();
    private final AtomicReference<PrintStream> replacement = new AtomicReference<>();
    private final UnsupportedOperationException cause = new UnsupportedOperationException(line1);
    private final AtomicReference<CapturedOutput> ref = new AtomicReference<>();

    @Test
    public void captureSystemOut() {
        //when
        final OngoingCapturedOutput captured = CaptureOutput.ofThread(() -> {
            writeOutput(System.out, line1, line2);
        }, maxAwaitMilliseconds);
        awaitLatch(captured.getCompletedLatch());
        //then
        assertThat(captured.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void captureSystemErr() {
        //when
        OngoingCapturedOutput captured = CaptureOutput.ofThread(() -> {
            writeOutput(System.err, line1, line2);
        }, maxAwaitMilliseconds);
        awaitLatch(captured.getCompletedLatch());
        //then
        assertThat(captured.getStdErr()).containsExactly(line1, line2);
    }

    @Test
    public void replaceSystemOut() {
        //given
        original.set(System.out);
        //when
        final OngoingCapturedOutput output = CaptureOutput.ofThread(() -> replacement.set(System.out), maxAwaitMilliseconds);
        //then
        assertThat(replacement).isNotSameAs(original);
        awaitLatch(output.getCompletedLatch());
    }

    @Test
    public void replaceSystemErr() {
        //given
        original.set(System.err);
        //when
        final OngoingCapturedOutput output = CaptureOutput.ofThread(() -> replacement.set(System.err), maxAwaitMilliseconds);
        //then
        assertThat(replacement).isNotSameAs(original);
        awaitLatch(output.getCompletedLatch());
    }

    @Test
    public void restoreSystemOut() {
        //given
        original.set(System.out);
        //when
        awaitLatch(
                CaptureOutput.ofThread(() -> {
                }, maxAwaitMilliseconds)
                        .getCompletedLatch());
        //then
        assertThat(System.out).isSameAs(original.get());
    }

    @Test
    public void restoreSystemErr() {
        //given
        original.set(System.err);
        //when
        awaitLatch(
                CaptureOutput.ofThread(() -> {
                }, maxAwaitMilliseconds)
                        .getCompletedLatch());
        //then
        assertThat(System.err).isSameAs(original.get());
    }

    @Test
    public void exceptionThrownIsAvailable() {
        //when
        final OngoingCapturedOutput capturedOutput = CaptureOutput.ofThread(() -> {
            throw cause;
        }, maxAwaitMilliseconds);
        awaitLatch(capturedOutput.getCompletedLatch());
        assertThat(capturedOutput.thrownException()).contains(cause);
    }

    @Test
    public void filtersToTargetThreadOut() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
        //when
        final OngoingCapturedOutput capturedOutput = CaptureOutput.ofThread(() -> {
            latchPair.releaseAndWait();
            System.out.println(line2);
        }, maxAwaitMilliseconds);
        awaitLatch(capturedOutput.getCompletedLatch());
        //then
        assertThat(capturedOutput.getStdOut()).containsExactly(line2);
    }

    @Test
    public void filtersToTargetThreadErr() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
        //when
        final OngoingCapturedOutput capturedOutput = CaptureOutput.ofThread(() -> {
            latchPair.releaseAndWait();
            System.err.println(line2);
        }, maxAwaitMilliseconds);
        awaitLatch(capturedOutput.getCompletedLatch());
        //then
        assertThat(capturedOutput.getStdErr()).containsExactly(line2);
    }

    @Test
    public void redirectFromOriginalOut() {
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final OngoingCapturedOutput copyOf = CaptureOutput.ofThread(() -> {
                writeOutput(System.out, line1, line2);
            }, maxAwaitMilliseconds);
            awaitLatch(copyOf.getCompletedLatch());
            ref.set(copyOf);
        }, maxAwaitMilliseconds);
        //then
        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdOut()).isEmpty();
    }

    @Test
    public void redirectFromOriginalErr() {
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final OngoingCapturedOutput copyOf = CaptureOutput.ofThread(() -> {
                writeOutput(System.err, line1, line2);
            }, maxAwaitMilliseconds);
            awaitLatch(copyOf.getCompletedLatch());
            ref.set(copyOf);
        }, maxAwaitMilliseconds);
        //then
        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdErr()).isEmpty();
    }

    @Test
    public void flushOut() {
        //given
        final SafeLatch ready = createLatch();
        final SafeLatch done = createLatch();
        final OngoingCapturedOutput capturedOutput =
                CaptureOutput.ofThread(() -> {
                    System.out.println(line1);
                    releaseLatch(ready);
                    awaitLatch(done);
                    System.out.println(line2);
                }, maxAwaitMilliseconds);
        awaitLatch(ready);
        //when
        final CapturedOutput initialOutput =
                capturedOutput.getCapturedOutputAndFlush();
        releaseLatch(done);
        awaitLatch(capturedOutput.getCompletedLatch());
        //then
        assertThat(initialOutput.getStdOut()).containsExactly(line1);
        assertThat(capturedOutput.getStdOut()).containsExactly(line2);
        assertThat(initialOutput.out().toString()).isEqualToIgnoringWhitespace(line1);
        assertThat(capturedOutput.out().toString()).isEqualToIgnoringWhitespace(line2);
    }

    @Test
    public void flushErr() {
        //given
        final SafeLatch ready = createLatch();
        final SafeLatch done = createLatch();
        final OngoingCapturedOutput capturedOutput =
                CaptureOutput.ofThread(() -> {
                    System.err.println(line1);
                    releaseLatch(ready);
                    awaitLatch(done);
                    System.err.println(line2);
                }, maxAwaitMilliseconds);
        awaitLatch(ready);
        //when
        final CapturedOutput initialOutput =
                capturedOutput.getCapturedOutputAndFlush();
        releaseLatch(done);
        awaitLatch(capturedOutput.getCompletedLatch());
        //then
        assertThat(initialOutput.getStdErr()).containsExactly(line1);
        assertThat(capturedOutput.getStdErr()).containsExactly(line2);
        assertThat(initialOutput.err().toString()).isEqualToIgnoringWhitespace(line1);
        assertThat(capturedOutput.err().toString()).isEqualToIgnoringWhitespace(line2);
    }

}
