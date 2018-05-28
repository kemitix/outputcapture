package net.kemitix.outputcapture.test;

import net.kemitix.outputcapture.CaptureOutput;
import net.kemitix.outputcapture.CapturedOutput;
import net.kemitix.outputcapture.OutputCaptureException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// CaptureOutput.ofAll(...)
public class SynchronousRedirectTest extends AbstractCaptureTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(MAX_TIMEOUT);

    @Test
    public void captureSystemOut() {
        //when
        final CapturedOutput captured = CaptureOutput.ofAll(() -> writeOutput(System.out, line1, line2));
        //then
        assertThat(captured.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void captureSystemErr() {
        //when
        final CapturedOutput captured = CaptureOutput.ofAll(() -> writeOutput(System.err, line1, line2));
        //then
        assertThat(captured.getStdErr()).containsExactly(line1, line2);
    }

    @Test
    public void replaceSystemOut() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        final AtomicReference<PrintStream> replacement = new AtomicReference<>();
        original.set(System.out);
        //when
        CaptureOutput.ofAll(() -> replacement.set(System.out));
        //then
        assertThat(replacement).isNotSameAs(original);
    }

    @Test
    public void replaceSystemErr() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        final AtomicReference<PrintStream> replacement = new AtomicReference<>();
        original.set(System.err);
        //when
        CaptureOutput.ofAll(() -> replacement.set(System.err));
        //then
        assertThat(replacement).isNotSameAs(original);
    }

    @Test
    public void restoreSystemOut() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        original.set(System.out);
        //when
        CaptureOutput.ofAll(this::doNothing);
        //then
        assertThat(System.out).isSameAs(original.get());
    }

    @Test
    public void restoreSystemErr() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        original.set(System.err);
        //when
        CaptureOutput.ofAll(this::doNothing);
        //then
        assertThat(System.err).isSameAs(original.get());
    }

    @Test
    public void wrappedInOutputCaptureException() {
        //given
        final UnsupportedOperationException cause = new UnsupportedOperationException(line1);
        //then
        assertThatThrownBy(() -> CaptureOutput.ofAll(() -> {
            throw cause;
        }))
                .isInstanceOf(OutputCaptureException.class)
                .hasCause(cause);
    }

    @Test
    public void capturesAllThreadsOut() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
        //when
        final CapturedOutput captured = CaptureOutput.ofAll(() -> {
            latchPair.releaseAndWait();
            writeOutput(System.out, line2);
        });
        //then
        assertThat(captured.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void capturesAllThreadsErr() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
        //when
        final CapturedOutput captured = CaptureOutput.ofAll(() -> {
            latchPair.releaseAndWait();
            writeOutput(System.err, line2);
        });
        //then
        assertThat(captured.getStdErr()).containsExactly(line1, line2);
    }

    @Test
    public void redirectFromOriginalOut() {
        //given
        final AtomicReference<CapturedOutput> ref = new AtomicReference<>();
        final AtomicBoolean finished = new AtomicBoolean(false);
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final CapturedOutput copyOf = CaptureOutput.ofAll(() -> {
                writeOutput(System.out, line1, line2);
                finished.set(true);
            });
            ref.set(copyOf);
        });
        //then
        assertThat(finished).isTrue();
        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdOut()).isEmpty();
    }

    @Test
    public void redirectFromOriginalErr() {
        //given
        final AtomicReference<CapturedOutput> ref = new AtomicReference<>();
        final AtomicBoolean finished = new AtomicBoolean(false);
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final CapturedOutput copyOf = CaptureOutput.ofAll(() -> {
                writeOutput(System.err, line1, line2);
                finished.set(true);
            });
            ref.set(copyOf);
        });
        //then
        assertThat(finished).isTrue();
        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdErr()).isEmpty();
    }

}
