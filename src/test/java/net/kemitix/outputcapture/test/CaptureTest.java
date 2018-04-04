package net.kemitix.outputcapture.test;

import lombok.SneakyThrows;
import net.kemitix.outputcapture.*;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class CaptureTest {

    //<editor-fold desc="fields">
    private static final long A_PERIOD = 200L;

    private static final long A_SHORT_PERIOD = 100L;

    private String line1 = "1:" + UUID.randomUUID().toString();

    private String line2 = "2:" + UUID.randomUUID().toString();
    //</editor-fold>

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
        final CapturedOutput capturedEcho = CaptureOutput.whileDoing(() -> {
            inner.set(CaptureOutput.copyOf(() -> {
                System.out.println(line1);
                System.err.println(line2);
                System.out.write('a');
            }));
        });
        //then
        assertThat(CaptureOutput.activeCount()).isEqualTo(0);
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
                System.out.println("started");
                releaseLatch(releaseLatch);
                awaitLatch(doneLatch);
                System.out.println("finished");
            }));
        });
        catchMe.submit(catchMe::shutdown);
        ignoreMe.awaitTermination(A_SHORT_PERIOD, TimeUnit.MILLISECONDS);
        catchMe.awaitTermination(A_SHORT_PERIOD, TimeUnit.MILLISECONDS);
        //then
        assertThat(reference.get()
                .getStdOut()).containsExactly("started", "finished");
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
        final ExecutorService subject = Executors.newSingleThreadExecutor();
        final AtomicReference<CapturedOutput> reference = new AtomicReference<>();
        //when
        monitor.submit(() -> {
            reference.set(CaptureOutput.of(() -> {
                subject.submit(() -> {
                    System.out.println("message");
                    System.out.write('x');
                });
                subject.submit(subject::shutdown);
            }));
        });
        monitor.submit(monitor::shutdown);
        subject.awaitTermination(A_PERIOD, TimeUnit.MILLISECONDS);
        monitor.awaitTermination(A_PERIOD, TimeUnit.MILLISECONDS);
        //then
        assertThat(reference.get()
                .getStdOut()).containsExactly("");
    }

    @Test(timeout = A_SHORT_PERIOD)
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
        //then
        releaseLatch(latch3);
        executor.awaitTermination(A_SHORT_PERIOD, TimeUnit.MILLISECONDS);
        assertThat(System.out).isSameAs(originalOut);
        assertThat(System.err).isSameAs(originalErr);
    }

    private void releaseLatch(final CountDownLatch latch) {
        latch.countDown();
    }

    private CountDownLatch createLatch() {
        return new CountDownLatch(1);
    }

    private void sleep(final long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            fail("sleep() interrupted");
        }
    }

    @SneakyThrows
    private void runOnAnotherThreadAndWait(final Runnable runnable) {
        final Thread thread = new Thread(runnable);
        thread.start();
        thread.join();
    }

    @Test(timeout = 200)
    public void canCaptureOutputAsynchronously() {
        //given
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final CountDownLatch latch1 = createLatch();
        final CountDownLatch latch2 = createLatch();
        final CountDownLatch latch3 = createLatch();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.ofThread(() -> {
            System.out.println("starting out");
            System.err.println("starting err");
            releaseLatch(latch1);
            awaitLatch(latch2);
            System.out.println("finished out");
            System.err.println("finished err");
            releaseLatch(latch3);
        });
        //then
        awaitLatch(latch1);
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly("starting out");
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly("starting err");
        releaseLatch(latch2);
        awaitLatch(latch3);
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly("starting out", "finished out");
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly("starting err", "finished err");
        ongoingCapturedOutput.await(A_PERIOD, TimeUnit.MILLISECONDS);
        assertThat(System.out).as("restore original out")
                .isSameAs(originalOut);
        assertThat(System.err).as("restore original err")
                .isSameAs(originalErr);
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
        assertThat(outerCaptured.getStdOut()).containsExactly(line2)
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
        assertThat(outerCaptured.getStdErr()).containsExactly(line2)
                .doesNotContain(line1);
    }

    @Test(timeout = 200)
    public void canFlushCapturedOutputWhenCapturingAsynchronously() {
        //given
        final CountDownLatch readyToFlush = createLatch();
        final CountDownLatch flushCompleted = createLatch();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.ofThread(() -> {
            System.out.println("starting out");
            System.err.println("starting err");
            releaseLatch(readyToFlush);
            awaitLatch(flushCompleted);
            System.out.println("finished out");
            System.err.println("finished err");
        });
        awaitLatch(readyToFlush);
        assertThat(ongoingCapturedOutput.getStdOut()).as("starting out").containsExactly("starting out");
        assertThat(ongoingCapturedOutput.getStdErr()).as("starting err").containsExactly("starting err");
        ongoingCapturedOutput.flush();
        releaseLatch(flushCompleted);
        awaitLatch(ongoingCapturedOutput.getCompletedLatch());
        //then
        assertThat(CaptureOutput.activeCount()).isEqualTo(0);
        assertThat(ongoingCapturedOutput.isShutdown()).isTrue();
        assertThat(ongoingCapturedOutput.getStdOut()).as("finished out").containsExactly("finished out");
        assertThat(ongoingCapturedOutput.getStdErr()).as("finished err").containsExactly("finished err");
    }

    @Test(timeout = 200)
    public void canCapturedOutputAndFlushWhenCapturingAsynchronously() {
        //given
        final CountDownLatch readyToFlush = createLatch();
        final CountDownLatch flushCompleted = createLatch();
        final CountDownLatch allDone = createLatch();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.ofThread(() -> {
            System.out.println("starting out");
            System.err.println("starting err");
            releaseLatch(readyToFlush);
            awaitLatch(flushCompleted);
            System.out.println("finished out");
            System.err.println("finished err");
            releaseLatch(allDone);
        });
        awaitLatch(readyToFlush);
        final CapturedOutput initialCapturedOutput = ongoingCapturedOutput.getCapturedOutputAndFlush();
        releaseLatch(flushCompleted);
        //then
        assertThat(initialCapturedOutput.getStdOut()).containsExactly("starting out");
        assertThat(initialCapturedOutput.getStdErr()).containsExactly("starting err");
        awaitLatch(allDone);
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly("finished out");
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly("finished err");
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

    @Ignore("broken") @Test(timeout = 200)
    public void canCaptureOutputAndCopyItToNormalOutputsWhenCapturingAsynchronously() {
        //given
        final CountDownLatch latch1 = createLatch();
        final CountDownLatch latch2 = createLatch();
        //when
        final CapturedOutput outerCaptured = CaptureOutput.copyWhileDoing(() -> {
            final OngoingCapturedOutput innerCaptured = CaptureOutput.copyOfThread(() -> {
                System.out.println(line1);
                releaseLatch(latch1);
            });
            awaitLatch(latch1);
            innerCaptured.await(A_PERIOD, TimeUnit.MILLISECONDS);
            System.out.println(line2);
            assertThat(innerCaptured.getStdOut()).containsExactly(line1)
                    .doesNotContain(line2);
            releaseLatch(latch2);
        });
        //then
        awaitLatch(latch2);
        assertThat(outerCaptured.getStdOut()).containsExactly(line1, line2);
    }
}
