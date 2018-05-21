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

// CaptureOutput.whileDoing(...)
public class AsynchronousRedirectTest extends AbstractCaptureTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(MAX_TIMEOUT);

    @Test
    public void captureSystemOut() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.whileDoing(() -> {
            latchPair.releaseAndWait();
            writeOutput(System.out, line2);
        }, MAX_TIMEOUT);
        awaitLatch(ongoing.getCompletedLatch());
        //then
        assertThat(ongoing.getStdOut()).containsExactly(line1, line2);
        assertThat(ongoing.executorIsShutdown()).isTrue();
    }

    @Test
    public void captureSystemErr() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.whileDoing(() -> {
            latchPair.releaseAndWait();
            writeOutput(System.err, line2);
        }, MAX_TIMEOUT);
        awaitLatch(ongoing.getCompletedLatch());
        //then
        assertThat(ongoing.getStdErr()).containsExactly(line1, line2);
        assertThat(ongoing.executorIsShutdown()).isTrue();
    }

    @Test
    public void replaceSystemOut() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        final AtomicReference<PrintStream> replacement = new AtomicReference<>();
        original.set(System.out);
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.whileDoing(() -> replacement.set(System.out), MAX_TIMEOUT);
        //then
        assertThat(replacement).isNotSameAs(original);
        awaitLatch(ongoing.getCompletedLatch());
        assertThat(ongoing.executorIsShutdown()).isTrue();
    }

    @Test
    public void replaceSystemErr() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        final AtomicReference<PrintStream> replacement = new AtomicReference<>();
        original.set(System.err);
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.whileDoing(() -> replacement.set(System.err), MAX_TIMEOUT);
        //then
        assertThat(replacement).isNotSameAs(original);
        awaitLatch(ongoing.getCompletedLatch());
        assertThat(ongoing.executorIsShutdown()).isTrue();
    }

    @Test
    public void restoreSystemOut() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        original.set(System.out);
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.whileDoing(this::doNothing, MAX_TIMEOUT);
        awaitLatch(ongoing.getCompletedLatch());
        //then
        assertThat(System.out).isSameAs(original.get());
        assertThat(ongoing.executorIsShutdown()).isTrue();
    }

    @Test
    public void restoreSystemErr() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        original.set(System.err);
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.whileDoing(this::doNothing, MAX_TIMEOUT);
        awaitLatch(ongoing.getCompletedLatch());
        //then
        assertThat(System.err).isSameAs(original.get());
        assertThat(ongoing.executorIsShutdown()).isTrue();
    }

    @Test
    public void exceptionThrownIsAvailable() {
        //given
        final UnsupportedOperationException cause = new UnsupportedOperationException(line1);
        // when
        final OngoingCapturedOutput ongoing = CaptureOutput.whileDoing(() -> {
            throw cause;
        }, MAX_TIMEOUT);
        awaitLatch(ongoing.getCompletedLatch());
        assertThat(ongoing.thrownException()).contains(cause);
        assertThat(ongoing.executorIsShutdown()).isTrue();
    }

    @Test
    public void capturesAllThreadsOut() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.whileDoing(() -> {
            latchPair.releaseAndWait();
            System.out.println(line2);
        }, MAX_TIMEOUT);
        awaitLatch(ongoing.getCompletedLatch());
        //then
        assertThat(ongoing.getStdOut()).containsExactly(line1, line2);
        assertThat(ongoing.executorIsShutdown()).isTrue();
    }

    @Test
    public void capturesAllThreadsErr() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.whileDoing(() -> {
            latchPair.releaseAndWait();
            System.err.println(line2);
        }, MAX_TIMEOUT);
        awaitLatch(ongoing.getCompletedLatch());
        //then
        assertThat(ongoing.getStdErr()).containsExactly(line1, line2);
        assertThat(ongoing.executorIsShutdown()).isTrue();
    }

    @Test
    public void redirectFromOriginalOut() {
        //given
        final AtomicReference<OngoingCapturedOutput> ref = new AtomicReference<>();
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final OngoingCapturedOutput ongoing =
                    CaptureOutput.whileDoing(() -> writeOutput(System.out, line1, line2), MAX_TIMEOUT);
            awaitLatch(ongoing.getCompletedLatch());
            ref.set(ongoing);
        }, MAX_TIMEOUT);
        //then
        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdOut()).isEmpty();
        assertThat(ref.get().executorIsShutdown()).isTrue();
    }

    @Test
    public void redirectFromOriginalErr() {
        //given
        final AtomicReference<OngoingCapturedOutput> ref = new AtomicReference<>();
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final OngoingCapturedOutput ongoing =
                    CaptureOutput.whileDoing(() -> writeOutput(System.err, line1, line2), MAX_TIMEOUT);
            awaitLatch(ongoing.getCompletedLatch());
            ref.set(ongoing);
        }, MAX_TIMEOUT);
        //then
        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdErr()).isEmpty();
        assertThat(ref.get().executorIsShutdown()).isTrue();
    }

    @Test
    public void flushOut() {
        //given
        final SafeLatch ready = createLatch();
        final SafeLatch done = createLatch();
        final OngoingCapturedOutput ongoing =
                CaptureOutput.whileDoing(() -> {
                    releaseLatch(ready);
                    awaitLatch(done);
                }, MAX_TIMEOUT);
        awaitLatch(ready);
        //when
        System.out.println(line1);
        final CapturedOutput initialOutput = ongoing.getCapturedOutputAndFlush();
        System.out.println(line2);
        releaseLatch(done);
        awaitLatch(ongoing.getCompletedLatch());
        //then
        assertThat(initialOutput.getStdOut()).containsExactly(line1);
        assertThat(ongoing.getStdOut()).containsExactly(line2);
        assertThat(initialOutput.out().toString()).isEqualToIgnoringWhitespace(line1);
        assertThat(ongoing.out().toString()).isEqualToIgnoringWhitespace(line2);
        assertThat(ongoing.executorIsShutdown()).isTrue();
    }

    @Test
    public void flushErr() {
        //given
        final SafeLatch ready = createLatch();
        final SafeLatch done = createLatch();
        final OngoingCapturedOutput ongoing =
                CaptureOutput.whileDoing(() -> {
                    releaseLatch(ready);
                    awaitLatch(done);
                }, MAX_TIMEOUT);
        awaitLatch(ready);
        //when
        System.err.println(line1);
        final CapturedOutput initialOutput = ongoing.getCapturedOutputAndFlush();
        System.err.println(line2);
        releaseLatch(done);
        awaitLatch(ongoing.getCompletedLatch());
        //then
        assertThat(initialOutput.getStdErr()).containsExactly(line1);
        assertThat(ongoing.getStdErr()).containsExactly(line2);
        assertThat(initialOutput.err().toString()).isEqualToIgnoringWhitespace(line1);
        assertThat(ongoing.err().toString()).isEqualToIgnoringWhitespace(line2);
        assertThat(ongoing.executorIsShutdown()).isTrue();
    }

}
