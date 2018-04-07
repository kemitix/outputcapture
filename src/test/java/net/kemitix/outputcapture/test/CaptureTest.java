package net.kemitix.outputcapture.test;

import lombok.SneakyThrows;
import lombok.val;
import net.kemitix.outputcapture.*;
import org.assertj.core.api.ThrowableAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

public class CaptureTest {

    //<editor-fold desc="fields">
    private static final long A_PERIOD = 200L;

    private static final long A_SHORT_PERIOD = 100L;
    private static final String STARTING_OUT = "starting out";
    private static final String STARTING_ERR = "starting err";
    private static final String FINISHED_OUT = "finished out";
    private static final String FINISHED_ERR = "finished err";

    private String line1 = "1:" + UUID.randomUUID().toString();

    private String line2 = "2:" + UUID.randomUUID().toString();
    //</editor-fold>

    @Rule
    public Timeout globalTimeout = Timeout.seconds(A_SHORT_PERIOD);

    @Before
    public void setUp() {
        assertThat(CaptureOutput.activeCount()).as("No existing captures").isZero();
    }

    @After
    public void tearDown() {
        assertThat(CaptureOutput.activeCount()).as("All captures removed").isZero();
    }

    @Test
    public void canCaptureSystemOut() {
        //when
        final CapturedOutput captured = CaptureOutput.of(() -> {
            System.out.println(line1);
            System.out.println(line2);
        });
        //then
        assertThat(captured.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void canCaptureSystemErr() {
        //when
        final CapturedOutput captured = CaptureOutput.of(() -> {
            System.err.println(line1);
            System.err.println(line2);
        });
        //then
        assertThat(captured.getStdErr()).containsExactly(line1, line2);
    }

    @Test
    public void canRestoreNormalSystemOut() {
        //when
        final CapturedOutput outerCaptured = CaptureOutput.of(() -> {
            final CapturedOutput innerCaptured = CaptureOutput.of(() -> {
                System.out.println(line1);
            });
            System.out.println(line2);
            assertThat(innerCaptured.getStdOut()).containsExactly(line1)
                    .doesNotContain(line2);
        });
        //then
        assertThat(outerCaptured.getStdOut()).containsExactly(line2)
                .doesNotContain(line1);
    }

    @Test
    public void canRestoreNormalSystemErr() {
        //when
        final CapturedOutput outerCaptured = CaptureOutput.of(() -> {
            final CapturedOutput innerCaptured = CaptureOutput.of(() -> {
                System.err.println(line1);
            });
            System.err.println(line2);
            assertThat(innerCaptured.getStdErr()).containsExactly(line1)
                    .doesNotContain(line2);
        });
        //then
        assertThat(outerCaptured.getStdErr()).containsExactly(line2)
                .doesNotContain(line1);
    }

    @Test
    public void canCaptureOutputAndCopyItToNormalOutputs() {
        //given
        final AtomicReference<CapturedOutput> inner = new AtomicReference<>();
        //when
        final OngoingCapturedOutput capturedEcho = CaptureOutput.copyWhileDoing(() -> {
            inner.set(CaptureOutput.copyOf(() -> {
                System.out.println(line1);
                System.err.println(line2);
                System.out.write('a');
            }));
        });
        //then
        awaitLatch(capturedEcho.getCompletedLatch());
        final CapturedOutput capturedOutput = inner.get();
        assertThat(capturedOutput.getStdOut()).as("inner std out written")
                .containsExactly(line1, "a");
        assertThat(capturedOutput.getStdErr()).as("inner std err written")
                .containsExactly(line2);
        assertThat(capturedEcho.getStdOut()).as("outer std out written")
                .containsExactly(line1, "a");
        assertThat(capturedEcho.getStdErr()).as("outer std err written")
                .containsExactly(line2);
    }

    @Test
    public void exceptionThrownInCallableAreWrappedInOutputCaptureException() {
        //given
        final UnsupportedOperationException cause = new UnsupportedOperationException(line1);
        //when
        final ThrowableAssert.ThrowingCallable action = () -> {
            CaptureOutput.of(() -> {
                throw cause;
            });
        };
        //then
        assertThatThrownBy(action).isInstanceOf(OutputCaptureException.class)
                .hasCause(cause);
    }

    @Test
    public void onlyCapturesOutputFromTargetRunnable() throws InterruptedException {
        //given
        final ExecutorService catchMe = Executors.newSingleThreadExecutor();
        final ExecutorService ignoreMe = Executors.newSingleThreadExecutor();
        final AtomicReference<CapturedOutput> reference = new AtomicReference<>();
        final CountDownLatch releaseLatch = createLatch();
        final CountDownLatch doneLatch = createLatch();
        //when
        ignoreMe.submit(() -> {
            awaitLatch(releaseLatch);
            System.out.println("ignore me");
            releaseLatch(doneLatch);
        });
        ignoreMe.submit(ignoreMe::shutdown);
        catchMe.submit(() -> {
            reference.set(CaptureOutput.of(() -> {
                asyncWithInterrupt(releaseLatch, doneLatch);
            }));
        });
        catchMe.submit(catchMe::shutdown);
        ignoreMe.awaitTermination(A_SHORT_PERIOD, TimeUnit.MILLISECONDS);
        catchMe.awaitTermination(A_SHORT_PERIOD, TimeUnit.MILLISECONDS);
        //then
        assertThat(reference.get()
                .getStdOut()).containsExactly(STARTING_OUT, FINISHED_OUT);
    }

    private void awaitLatch(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new OutputCaptureException(e);
        }
    }

    @Test
    public void capturesOutputOnRequiredThread() {
        //given
        final AtomicReference<CapturedOutput> capturedOutput = new AtomicReference<>();
        //when
        runOnAnotherThreadAndWait(() -> {
            capturedOutput.set(CaptureOutput.of(() -> {
                System.out.println("message");
                System.out.write('x');
            }));
        });
        //then
        assertThat(capturedOutput.get()
                .getStdOut()).containsExactly("message", "x");
    }

    @Test
    public void ignoresOutputFromOtherThreads() throws InterruptedException {
        //given
        final ExecutorService monitor = Executors.newSingleThreadExecutor();
        final AtomicReference<CapturedOutput> reference = new AtomicReference<>();
        //when
        monitor.submit(() -> {
            reference.set(CaptureOutput.of(() -> {
                final ExecutorService subject = Executors.newSingleThreadExecutor();
                subject.submit(() -> {
                    System.out.println("message");
                    System.out.write('x');
                    subject.shutdown();
                });
                subject.awaitTermination(A_PERIOD, TimeUnit.MILLISECONDS);
            }));
            monitor.shutdown();
        });
        monitor.awaitTermination(A_PERIOD, TimeUnit.MILLISECONDS);
        //then
        final CapturedOutput capturedOutput = reference.get();
        assertThat(capturedOutput.getStdOut()).containsExactly("");
    }

    @Test
    public void canRestoreSystemOutAndErrWhenMultipleOutputCapturesOverlap() throws InterruptedException {
        //given
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final CountDownLatch latch1 = createLatch();
        final CountDownLatch latch2 = createLatch();
        final CountDownLatch latch3 = createLatch();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            awaitLatch(latch1);
            CaptureOutput.of(() -> {
                releaseLatch(latch2);
                awaitLatch(latch3);
            });
        });
        executor.submit(executor::shutdown);
        //when
        CaptureOutput.of(() -> {
            releaseLatch(latch1);
            awaitLatch(latch2);
        });
        releaseLatch(latch3);
        executor.awaitTermination(A_SHORT_PERIOD, TimeUnit.MILLISECONDS);
        //then
        assertThat(System.out).isSameAs(originalOut);
        assertThat(System.err).isSameAs(originalErr);
    }

    private void releaseLatch(final CountDownLatch latch) {
        latch.countDown();
    }

    private CountDownLatch createLatch() {
        return new CountDownLatch(1);
    }

    @SneakyThrows
    private void runOnAnotherThreadAndWait(final Runnable runnable) {
        final Thread thread = new Thread(runnable);
        thread.start();
        thread.join();
    }

    @Test
    public void canCaptureOutputAsynchronously() {
        //given
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final CountDownLatch latch1 = createLatch();
        final CountDownLatch latch2 = createLatch();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.ofThread(() -> {
            asyncWithInterrupt(latch1, latch2);
        });
        awaitLatch(latch1);
        final Stream<String> stdOut = ongoingCapturedOutput.getStdOut();
        final Stream<String> stdErr = ongoingCapturedOutput.getStdErr();
        releaseLatch(latch2);
        awaitLatch(ongoingCapturedOutput.getCompletedLatch());
        //then
        assertThat(stdOut).containsExactly(STARTING_OUT);
        assertThat(stdErr).containsExactly(STARTING_ERR);
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(STARTING_OUT, FINISHED_OUT);
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly(STARTING_ERR, FINISHED_ERR);
        awaitLatch(ongoingCapturedOutput.getCompletedLatch());
        assertThat(System.out).as("restore original out").isSameAs(originalOut);
        assertThat(System.err).as("restore original err").isSameAs(originalErr);
    }

    private void asyncWithInterrupt(CountDownLatch latch1, CountDownLatch latch2) {
        System.out.println(STARTING_OUT);
        System.err.println(STARTING_ERR);
        releaseLatch(latch1);
        awaitLatch(latch2);
        System.out.println(FINISHED_OUT);
        System.err.println(FINISHED_ERR);
    }

    @Test
    public void canRestoreNormalSystemOutWhenCapturingAsynchronously() {
        //when
        final CapturedOutput outerCaptured = CaptureOutput.of(() -> {
            final OngoingCapturedOutput innerCaptured = CaptureOutput.ofThread(() -> {
                System.out.println(line1);
            });
            innerCaptured.await(A_PERIOD, TimeUnit.MILLISECONDS);
            System.out.println(line2);
            assertThat(innerCaptured.getStdOut()).containsExactly(line1)
                    .doesNotContain(line2);
        });
        //then
        assertThat(outerCaptured.getStdOut())
                .containsExactly(line2)
                .doesNotContain(line1);
    }

    @Test
    public void canRestoreNormalSystemErrWhenCapturingAsynchronously() {
        //when
        final CapturedOutput outerCaptured = CaptureOutput.of(() -> {
            final OngoingCapturedOutput innerCaptured = CaptureOutput.ofThread(() -> {
                System.err.println(line1);
            });
            innerCaptured.await(A_PERIOD, TimeUnit.MILLISECONDS);
            System.err.println(line2);
            assertThat(innerCaptured.getStdErr()).containsExactly(line1)
                    .doesNotContain(line2);
        });
        //then
        assertThat(outerCaptured.getStdErr())
                .containsExactly(line2)
                .doesNotContain(line1);
    }

    @Test
    public void canFlushCapturedOutputWhenCapturingAsynchronously() {
        //given
        final CountDownLatch readyToFlush = createLatch();
        final CountDownLatch flushCompleted = createLatch();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.copyOfThread(() -> {
            asyncWithInterrupt(readyToFlush, flushCompleted);
        });
        awaitLatch(readyToFlush);
        final Stream<String> stdOut = ongoingCapturedOutput.getStdOut();
        final Stream<String> stdErr = ongoingCapturedOutput.getStdErr();
        ongoingCapturedOutput.flush();
        releaseLatch(flushCompleted);
        awaitLatch(ongoingCapturedOutput.getCompletedLatch());
        //then
        assertThat(stdOut).containsExactly(STARTING_OUT);
        assertThat(stdErr).containsExactly(STARTING_ERR);
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(FINISHED_OUT);
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly(FINISHED_ERR);
    }

    @Test
    public void canCapturedOutputAndFlushWhenCapturingAsynchronously() {
        //given
        val readyToFlush = createLatch();
        val flushCompleted = createLatch();
        //when
        val ongoingCapturedOutput = CaptureOutput.ofThread(() -> {
            asyncWithInterrupt(readyToFlush, flushCompleted);
        });
        awaitLatch(readyToFlush);
        val initialCapturedOutput = ongoingCapturedOutput.getCapturedOutputAndFlush();
        releaseLatch(flushCompleted);
        //then
        try {
            assertThat(initialCapturedOutput.getStdOut()).containsExactly(STARTING_OUT);
            assertThat(initialCapturedOutput.getStdErr()).containsExactly(STARTING_ERR);
        } finally {
            awaitLatch(ongoingCapturedOutput.getCompletedLatch());
        }
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(FINISHED_OUT);
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly(FINISHED_ERR);
    }

    @Test
    public void canWaitForThreadToComplete() {
        //given
        final CountDownLatch latch = createLatch();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.ofThread(() -> {
            awaitLatch(latch);
        });
        //then
        assertThat(ongoingCapturedOutput.isRunning()).as("isRunning = true")
                .isTrue();
        assertThat(ongoingCapturedOutput.isShutdown()).as("isShutdown = false")
                .isFalse();
        releaseLatch(latch);
        ongoingCapturedOutput.await(A_SHORT_PERIOD, TimeUnit.MILLISECONDS);
        assertThat(ongoingCapturedOutput.isRunning()).as("isRunning = false")
                .isFalse();
        assertThat(ongoingCapturedOutput.isShutdown()).as("isShutdown = true")
                .isTrue();
    }

    @Test
    public void exceptionThrownInThreadIsAvailableToOngoingCapturedOutput() {
        //given
        final OutputCaptureException outputCaptureException = new OutputCaptureException("");
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.ofThread(() -> {
            throw outputCaptureException;
        });
        //then
        ongoingCapturedOutput.await(A_PERIOD, TimeUnit.MILLISECONDS);
        assertThat(ongoingCapturedOutput.thrownException()).contains(outputCaptureException);
    }

    @Test
    public void canCaptureCopyOfThread() {
        //when
        val capturedOutput = CaptureOutput.copyOfThread(() -> {
            System.out.println(line1);
        });
        //then
        awaitLatch(capturedOutput.getCompletedLatch());
        assertThat(capturedOutput.isShutdown()).isTrue();
        assertThat(capturedOutput.getStdOut()).containsExactly(line1);
    }


    @Test
    public void canCaptureCopyWhileDoing() {
        //given
        final CountDownLatch ready = createLatch();
        final CountDownLatch done = createLatch();
        val capturedOutput = CaptureOutput.copyWhileDoing(() -> {
            releaseLatch(ready);
            awaitLatch(done);
        });
        awaitLatch(ready);
        assertThat(done.getCount()).isNotZero();
        assertThat(CaptureOutput.activeCount()).isEqualTo(1);
        //when
        System.out.println(line1);
        releaseLatch(done);
        //then
        assertThat(done.getCount()).isZero();
        awaitLatch(capturedOutput.getCompletedLatch());
        assertThat(capturedOutput.isShutdown()).isTrue();
        assertThat(capturedOutput.getStdOut()).containsExactly(line1);
    }


    @Test
    public void canCaptureOutputAndCopyItToNormalOutputsWhenCapturingAsynchronously() {
        //when
        val outerCaptured = CaptureOutput.copyWhileDoing(() -> {
            assertThat(CaptureOutput.activeCount()).isEqualTo(1);
            val innerCaptured = CaptureOutput.copyOfThread(() -> {
                assertThat(CaptureOutput.activeCount()).isEqualTo(2);
                System.out.println(line1);
            });
            innerCaptured.await(A_PERIOD, TimeUnit.MILLISECONDS);
            awaitLatch(innerCaptured.getCompletedLatch());
            assertThat(CaptureOutput.activeCount()).isEqualTo(1);
            System.out.println(line2);
            assertThat(innerCaptured.getStdOut()).as("inner")
                    .containsExactly(line1, line2)
                    .doesNotContain(line2);
        });
        //then
        awaitLatch(outerCaptured.getCompletedLatch());
        assertThat(outerCaptured.getStdOut()).as("outer").containsExactly(line1, line2);
    }

}
