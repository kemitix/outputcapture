package net.kemitix.outputcapture.test;

import net.kemitix.outputcapture.CaptureOutput;
import net.kemitix.outputcapture.CapturedOutput;
import net.kemitix.outputcapture.OutputCaptureException;
import net.kemitix.outputcapture.SafeLatch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// CaptureOutput.of(...)
public class SynchronousFilteredRedirectTest extends AbstractCaptureTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(MAX_TIMEOUT);

    @Test
    public void captureSystemOut() {
        //when
        final CapturedOutput captured = CaptureOutput.of(() -> writeOutput(System.out, line1, line2));
        //then
        assertThat(captured.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void captureSystemErr() {
        //when
        final CapturedOutput captured = CaptureOutput.of(() -> writeOutput(System.err, line1, line2));
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
        CaptureOutput.of(() -> replacement.set(System.out));
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
        CaptureOutput.of(() -> replacement.set(System.err));
        //then
        assertThat(replacement).isNotSameAs(original);
    }

    @Test
    public void restoreSystemOut() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        original.set(System.out);
        //when
        CaptureOutput.of(() -> {
        });
        //then
        assertThat(System.out).isSameAs(original.get());
    }

    @Test
    public void restoreSystemErr() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        original.set(System.err);
        //when
        CaptureOutput.of(this::doNothing);
        //then
        assertThat(System.err).isSameAs(original.get());
    }

    @Test
    public void filtersToTargetThreadOut() {
        //given
        final ExecutorService catchMe = Executors.newSingleThreadExecutor();
        final ExecutorService ignoreMe = Executors.newSingleThreadExecutor();
        final AtomicReference<CapturedOutput> reference = new AtomicReference<>();
        final SafeLatch ready = createLatch();
        final SafeLatch done = createLatch();
        final SafeLatch finished = createLatch();
        //when
        catchMe.submit(() -> captureThis(ready, done, reference, finished, catchMe));
        ignoreMe.submit(() -> ignoreThis(ready, done, ignoreMe));
        //then
        awaitLatch(finished);
        final CapturedOutput capturedOutput = reference.get();
        assertThat(capturedOutput.getStdOut()).containsExactly(STARTING_OUT, FINISHED_OUT);
    }

    @Test
    public void filtersToTargetThreadErr() {
        //given
        final ExecutorService catchMe = Executors.newSingleThreadExecutor();
        final ExecutorService ignoreMe = Executors.newSingleThreadExecutor();
        final AtomicReference<CapturedOutput> reference = new AtomicReference<>();
        final SafeLatch ready = createLatch();
        final SafeLatch done = createLatch();
        final SafeLatch finished = createLatch();
        //when
        catchMe.submit(() -> captureThis(ready, done, reference, finished, catchMe));
        ignoreMe.submit(() -> ignoreThis(ready, done, ignoreMe));
        //then
        awaitLatch(finished);
        final CapturedOutput capturedOutput = reference.get();
        assertThat(capturedOutput.getStdErr()).containsExactly(STARTING_ERR, FINISHED_ERR);
    }

    @Test
    public void redirectFromOriginalOut() {
        //given
        final AtomicReference<CapturedOutput> ref = new AtomicReference<>();
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final CapturedOutput copyOf = CaptureOutput.of(() -> writeOutput(System.out, line1, line2));
            ref.set(copyOf);
        });
        //then
        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdOut()).isEmpty();
    }

    @Test
    public void redirectFromOriginalErr() {
        //given
        final AtomicReference<CapturedOutput> ref = new AtomicReference<>();
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final CapturedOutput copyOf = CaptureOutput.of(() -> writeOutput(System.err, line1, line2));
            ref.set(copyOf);
        });
        //then
        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdErr()).isEmpty();
    }

    @Test
    public void wrappedInOutputCaptureException() {
        //given
        final UnsupportedOperationException cause = new UnsupportedOperationException(line1);
        //then
        assertThatThrownBy(() -> CaptureOutput.of(() -> {
            throw cause;
        }))
                .isInstanceOf(OutputCaptureException.class)
                .hasCause(cause);
    }

    private void captureThis(
            final SafeLatch ready,
            final SafeLatch done,
            final AtomicReference<CapturedOutput> reference,
            final SafeLatch finished,
            final ExecutorService catchMe
    ) {
        final CapturedOutput capturedOutput = CaptureOutput.of(() -> asyncWithInterrupt(ready, done));
        reference.set(capturedOutput);
        releaseLatch(finished);
        catchMe.shutdown();
    }

    private void ignoreThis(
            final SafeLatch ready,
            final SafeLatch done,
            final ExecutorService ignoreMe
    ) {
        awaitLatch(ready);
        System.out.println("ignore me");
        releaseLatch(done);
        ignoreMe.shutdown();
    }


}
