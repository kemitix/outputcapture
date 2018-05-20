package net.kemitix.outputcapture.test;

import net.kemitix.outputcapture.CaptureOutput;
import net.kemitix.outputcapture.CapturedOutput;
import net.kemitix.outputcapture.OutputCaptureException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// CaptureOutput.copyOf(...)
public class SynchronousFilteredCopy extends AbstractCaptureTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(100L);

    private final AtomicReference<PrintStream> original = new AtomicReference<>();
    private final AtomicReference<PrintStream> replacement = new AtomicReference<>();
    private final UnsupportedOperationException cause = new UnsupportedOperationException(line1);
    private final AtomicReference<CapturedOutput> ref = new AtomicReference<>();

    @Test
    public void captureSystemOut() {
        //when
        final CapturedOutput captured = CaptureOutput.copyOf(() -> {
            writeOutput(System.out, line1, line2);
        }, maxAwaitMilliseconds);
        //then
        assertThat(captured.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void captureSystemErr() {
        //when
        final CapturedOutput captured = CaptureOutput.copyOf(() -> {
            writeOutput(System.err, line1, line2);
        }, maxAwaitMilliseconds);
        //then
        assertThat(captured.getStdErr()).containsExactly(line1, line2);
    }

    @Test
    public void replaceSystemOut() {
        //given
        original.set(System.out);
        //when
        CaptureOutput.copyOf(() -> replacement.set(System.out), maxAwaitMilliseconds);
        //then
        assertThat(replacement).isNotSameAs(original);
    }

    @Test
    public void replaceSystemErr() {
        //given
        original.set(System.err);
        //when
        CaptureOutput.copyOf(() -> replacement.set(System.err), maxAwaitMilliseconds);
        //then
        assertThat(replacement).isNotSameAs(original);
    }

    @Test
    public void restoreSystemOut() {
        //given
        original.set(System.out);
        //when
        CaptureOutput.copyOf(() -> {
        }, maxAwaitMilliseconds);
        //then
        assertThat(System.out).isSameAs(original.get());
    }

    @Test
    public void restoreSystemErr() {
        //given
        original.set(System.err);
        //when
        CaptureOutput.copyOf(() -> {
        }, maxAwaitMilliseconds);
        //then
        assertThat(System.err).isSameAs(original.get());
    }

    @Test
    public void wrappedInOutputCaptureException() {
        //then
        assertThatThrownBy(() -> CaptureOutput.copyOf(() -> {
            throw cause;
        }, maxAwaitMilliseconds))
                .isInstanceOf(OutputCaptureException.class)
                .hasCause(cause);
    }

    @Test
    public void filtersToTargetThreadOut() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
        //when
        final CapturedOutput capturedOutput = CaptureOutput.copyOf(() -> {
            latchPair.releaseAndWait();
            System.out.println(line2);
        }, maxAwaitMilliseconds);
        //then
        assertThat(capturedOutput.getStdOut()).containsExactly(line2);
    }

    @Test
    public void filtersToTargetThreadErr() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
        //when
        final CapturedOutput capturedOutput = CaptureOutput.copyOf(() -> {
            latchPair.releaseAndWait();
            System.err.println(line2);
        }, maxAwaitMilliseconds);
        //then
        assertThat(capturedOutput.getStdErr()).containsExactly(line2);
    }

    @Test
    public void copyToOriginalOut() {
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final CapturedOutput copyOf = CaptureOutput.copyOf(() -> {
                writeOutput(System.out, line1, line2);
            }, maxAwaitMilliseconds);
            ref.set(copyOf);
        }, maxAwaitMilliseconds);
        //then
        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void copyToOriginalErr() {
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final CapturedOutput copyOf = CaptureOutput.copyOf(() -> {
                writeOutput(System.err, line1, line2);
            }, maxAwaitMilliseconds);
            ref.set(copyOf);
        }, maxAwaitMilliseconds);
        //then
        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdErr()).containsExactly(line1, line2);
    }

}
