package net.kemitix.outputcapture.test;

import net.kemitix.outputcapture.CaptureOutput;
import net.kemitix.outputcapture.CapturedOutput;
import net.kemitix.outputcapture.OngoingCapturedOutput;
import net.kemitix.outputcapture.SafeLatch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class AsynchronousCopyTest extends AbstractCaptureTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(MAX_TIMEOUT);

    @Test
    public void captureSystemOut() {
        //when
        final OngoingCapturedOutput ongoing =
                CaptureOutput.copyWhileDoing(() -> writeOutput(System.out, line1, line2), MAX_TIMEOUT);
        ongoing.join();
        //then
        assertThat(ongoing.executorIsShutdown()).isTrue();
        assertThat(ongoing.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void captureSystemErr() {
        //when
        final OngoingCapturedOutput ongoing =
                CaptureOutput.copyWhileDoing(() -> writeOutput(System.err, line1, line2), MAX_TIMEOUT);
        ongoing.join();
        //then
        assertThat(ongoing.executorIsShutdown()).isTrue();
        assertThat(ongoing.getStdErr()).containsExactly(line1, line2);
    }

    @Test
    public void replaceSystemOut() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        final AtomicReference<PrintStream> replacement = new AtomicReference<>();
        original.set(System.out);
        //when
        final OngoingCapturedOutput ongoing =
                CaptureOutput.copyWhileDoing(() -> replacement.set(System.out), MAX_TIMEOUT);
        ongoing.join();
        //then
        assertThat(ongoing.executorIsShutdown()).isTrue();
        assertThat(replacement).isNotSameAs(original);
    }

    @Test
    public void replaceSystemErr() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        final AtomicReference<PrintStream> replacement = new AtomicReference<>();
        original.set(System.err);
        //when
        final OngoingCapturedOutput ongoing =
                CaptureOutput.copyWhileDoing(() -> replacement.set(System.err), MAX_TIMEOUT);
        ongoing.join();
        //then
        assertThat(ongoing.executorIsShutdown()).isTrue();
        assertThat(replacement).isNotSameAs(original);
    }

    @Test
    public void restoreSystemOut() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        original.set(System.out);
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.copyWhileDoing(this::doNothing, MAX_TIMEOUT);
        ongoing.join();
        //then
        assertThat(ongoing.executorIsShutdown()).isTrue();
        assertThat(System.out).isSameAs(original.get());
    }

    @Test
    public void restoreSystemErr() {
        //given
        final AtomicReference<PrintStream> original = new AtomicReference<>();
        original.set(System.err);
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.copyWhileDoing(this::doNothing, MAX_TIMEOUT);
        ongoing.join();
        //then
        assertThat(ongoing.executorIsShutdown()).isTrue();
        assertThat(System.err).isSameAs(original.get());
    }

    @Test
    public void exceptionThrownIsAvailable() {
        //given
        final UnsupportedOperationException cause = new UnsupportedOperationException(line1);
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.copyWhileDoing(() -> {
            throw cause;
        }, MAX_TIMEOUT);
        ongoing.join();
        //then
        assertThat(ongoing.executorIsShutdown()).isTrue();
        assertThat(ongoing.thrownException()).contains(cause);
    }

    @Test
    public void capturesAllThreadsOut() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.copyWhileDoing(() -> {
            latchPair.releaseAndWait();
            System.out.println(line2);
        }, MAX_TIMEOUT);
        ongoing.join();
        //then
        assertThat(ongoing.executorIsShutdown()).isTrue();
        assertThat(ongoing.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void capturesAllThreadsErr() {
        //given
        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
        //when
        final OngoingCapturedOutput ongoing = CaptureOutput.copyWhileDoing(() -> {
            latchPair.releaseAndWait();
            System.err.println(line2);
        }, MAX_TIMEOUT);
        ongoing.join();
        //then
        assertThat(ongoing.executorIsShutdown()).isTrue();
        assertThat(ongoing.getStdErr()).containsExactly(line1, line2);
    }

    @Test
    public void copyToOriginalOut() {
        //given
        final AtomicReference<OngoingCapturedOutput> ref = new AtomicReference<>();
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final OngoingCapturedOutput ongoing =
                    CaptureOutput.copyWhileDoing(() -> writeOutput(System.out, line1, line2), MAX_TIMEOUT);
            ongoing.join();
            ref.set(ongoing);
        });
        //then
        assertThat(ref.get().executorIsShutdown()).isTrue();
        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void copyToOriginalErr() {
        //given
        final AtomicReference<OngoingCapturedOutput> ref = new AtomicReference<>();
        //when
        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
            final OngoingCapturedOutput ongoing = CaptureOutput.copyWhileDoing(() -> {
                writeOutput(System.err, line1, line2);
            }, MAX_TIMEOUT);
            ongoing.join();
            ref.set(ongoing);
        });
        //then
        assertThat(ref.get().executorIsShutdown()).isTrue();
        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
        assertThat(capturedOutput.getStdErr()).containsExactly(line1, line2);
    }

    @Test
    public void flushOut() {
        //given
        final SafeLatch ready = createLatch();
        final SafeLatch done = createLatch();
        final OngoingCapturedOutput ongoing =
                CaptureOutput.copyWhileDoing(() -> {
                    releaseLatch(ready);
                    awaitLatch(done);
                }, MAX_TIMEOUT);
        awaitLatch(ready);
        //when
        System.out.println(line1);
        final CapturedOutput initialOutput =
                ongoing.getCapturedOutputAndFlush();
        System.out.println(line2);
        releaseLatch(done);
        ongoing.join();
        //then
        assertThat(ongoing.executorIsShutdown()).isTrue();
        assertThat(initialOutput.getStdOut()).containsExactly(line1);
        assertThat(ongoing.getStdOut()).containsExactly(line2);
        assertThat(initialOutput.out().toString()).isEqualToIgnoringWhitespace(line1);
        assertThat(ongoing.out().toString()).isEqualToIgnoringWhitespace(line2);
    }

    @Test
    public void flushErr() {
        //given
        final SafeLatch ready = createLatch();
        final SafeLatch done = createLatch();
        final OngoingCapturedOutput ongoing =
                CaptureOutput.copyWhileDoing(() -> {
                    releaseLatch(ready);
                    awaitLatch(done);
                }, MAX_TIMEOUT);
        awaitLatch(ready);
        //when
        System.err.println(line1);
        final CapturedOutput initialOutput =
                ongoing.getCapturedOutputAndFlush();
        System.err.println(line2);
        releaseLatch(done);
        ongoing.join();
        //then
        assertThat(ongoing.executorIsShutdown()).isTrue();
        assertThat(initialOutput.getStdErr()).containsExactly(line1);
        assertThat(ongoing.getStdErr()).containsExactly(line2);
        assertThat(initialOutput.err().toString()).isEqualToIgnoringWhitespace(line1);
        assertThat(ongoing.err().toString()).isEqualToIgnoringWhitespace(line2);
    }

}
