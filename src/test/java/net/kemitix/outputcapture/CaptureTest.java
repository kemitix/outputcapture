/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Paul Campbell
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.kemitix.outputcapture;

import lombok.SneakyThrows;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests for capturing output.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class CaptureTest {

    private static final long A_PERIOD = 200L;

    private static final long A_SHORT_PERIOD = 100L;

    private static final long A_LONG_PERIOD = 1000L;

    private String line1;

    private String line2;

    private ThrowingCallable asyncRunnable;

    @Mock
    private Function<Integer, CountDownLatch> latchFactory;

    @Mock
    private CountDownLatch latch;

    @Mock
    private Router router;

    @Mock
    private ByteArrayOutputStream capturedOut;

    @Mock
    private ByteArrayOutputStream capturedErr;

    private AtomicReference<Exception> thrownException = new AtomicReference<>();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        line1 = "1:" + randomText();
        line2 = "2:" + randomText();
        asyncRunnable = () -> {
            System.out.println("starting out");
            System.err.println("starting err");
            sleep(A_PERIOD);
            System.out.println("finished out");
            System.err.println("finished err");
        };
    }

    @Test
    public void canCaptureSystemOut() {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        //when
        final CapturedOutput captured = captureOutput.of(() -> {
            System.out.println(line1);
            System.out.println(line2);
        });
        //then
        assertThat(captured.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void canCaptureSystemErr() {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        //when
        final CapturedOutput captured = captureOutput.of(() -> {
            System.err.println(line1);
            System.err.println(line2);
        });
        //then
        assertThat(captured.getStdErr()).containsExactly(line1, line2);
    }

    @Test
    public void canRestoreNormalSystemOut() throws Exception {
        //given
        final CaptureOutput outer = new CaptureOutput();
        final CaptureOutput inner = new CaptureOutput();
        //when
        final CapturedOutput outerCaptured = outer.of(() -> {
            final CapturedOutput innerCaptured = inner.of(() -> {
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
    public void canRestoreNormalSystemErr() throws Exception {
        //given
        final CaptureOutput outer = new CaptureOutput();
        final CaptureOutput inner = new CaptureOutput();
        //when
        final CapturedOutput outerCaptured = outer.of(() -> {
            final CapturedOutput innerCaptured = inner.of(() -> {
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
        final CaptureOutput captureOutput = new CaptureOutput();
        final CaptureOutput captureCopy = new CaptureOutput();
        final AtomicReference<CapturedOutput> inner = new AtomicReference<>();
        //when
        final CapturedOutput capturedEcho = captureCopy.of(() -> {
            inner.set(captureOutput.copyOf(() -> {
                System.out.println(line1);
                System.err.println(line2);
                System.out.write("a".getBytes()[0]);
            }));
        });
        //then
        assertThat(capturedEcho.getStdOut()).containsExactly(line1, "a");
        assertThat(capturedEcho.getStdErr()).containsExactly(line2);
        assertThat(inner.get()
                        .getStdOut()).containsExactly(line1, "a");
        assertThat(inner.get()
                        .getStdErr()).containsExactly(line2);
    }

    @Test
    public void exceptionThrownInCallableAreWrappedInOutputCaptureException() {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        final UnsupportedOperationException cause = new UnsupportedOperationException(line1);
        //when
        final ThrowableAssert.ThrowingCallable action = () -> {
            captureOutput.of(() -> {
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
        final CaptureOutput captureOutput = new CaptureOutput();
        final ExecutorService catchMe = Executors.newSingleThreadExecutor();
        final ExecutorService ignoreMe = Executors.newSingleThreadExecutor();
        final AtomicReference<CapturedOutput> reference = new AtomicReference<>();
        //when
        ignoreMe.submit(() -> {
            sleep(A_SHORT_PERIOD);
            System.out.println("ignore me");
        });
        catchMe.submit(() -> {
            reference.set(captureOutput.of(() -> {
                System.out.println("started");
                sleep(A_PERIOD);
                System.out.println("finished");
            }));
        });
        ignoreMe.awaitTermination(A_LONG_PERIOD, TimeUnit.MILLISECONDS);
        catchMe.awaitTermination(A_LONG_PERIOD, TimeUnit.MILLISECONDS);
        //then
        assertThat(reference.get()
                            .getStdOut()).containsExactly("started", "finished");
    }

    @Test
    public void capturesOutputOnRequiredThread() {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        final AtomicReference<CapturedOutput> capturedOutput = new AtomicReference<>();
        //when
        runOnThreadAndWait(() -> {
            capturedOutput.set(captureOutput.of(() -> {
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
        final CaptureOutput captureOutput = new CaptureOutput();
        final ExecutorService monitor = Executors.newSingleThreadExecutor();
        final ExecutorService subject = Executors.newSingleThreadExecutor();
        final AtomicReference<CapturedOutput> reference = new AtomicReference<>();
        //when
        monitor.submit(() -> {
            reference.set(captureOutput.of(() -> {
                subject.submit(() -> {
                    System.out.println("message");
                    System.out.write('x');
                });
            }));
        });
        subject.awaitTermination(A_LONG_PERIOD, TimeUnit.MILLISECONDS);
        monitor.awaitTermination(A_LONG_PERIOD, TimeUnit.MILLISECONDS);
        //then
        assertThat(reference.get()
                            .getStdOut()).containsExactly("");
    }

    @Test
    public void exceptionIsThrownWhenMultipleOutputCapturesOverlap() throws InterruptedException {
        //given
        final Runnable runnable = () -> {
            sleep(A_PERIOD);
            new CaptureOutput().of(() -> {
                sleep(A_PERIOD);
            });
        };
        final Thread thread = new Thread(runnable);
        thread.start();
        //when
        final ThrowableAssert.ThrowingCallable action = () -> {
            new CaptureOutput().of(() -> {
                sleep(A_PERIOD + A_SHORT_PERIOD);
            });
        };
        //then
        assertThatThrownBy(action).hasNoCause()
                                  .isInstanceOf(OutputCaptureException.class);
        thread.join();
    }

    private void sleep(final long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            fail("sleep() interrupted");
        }
    }

    @SneakyThrows
    private void runOnThreadAndWait(final Runnable runnable) {
        final Thread thread = new Thread(runnable);
        thread.start();
        thread.join();
    }

    private String randomText() {
        return UUID.randomUUID()
                   .toString();
    }

    @Test
    public void canCaptureOutputAsynchronously() throws InterruptedException {
        //given
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final CaptureOutput captureOutput = new CaptureOutput();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = captureOutput.ofThread(asyncRunnable);
        //then
        sleep(A_SHORT_PERIOD);
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly("starting out");
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly("starting err");
        sleep(A_PERIOD);
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly("starting out", "finished out");
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly("starting err", "finished err");
        ongoingCapturedOutput.await(A_PERIOD, TimeUnit.MILLISECONDS);
        assertThat(System.out).as("restore original out")
                              .isSameAs(originalOut);
        assertThat(System.err).as("restore original err")
                              .isSameAs(originalErr);
    }

    @Test
    public void canRestoreNormalSystemOutWhenCapturingAsynchronously() throws Exception {
        //given
        final CaptureOutput outer = new CaptureOutput();
        final CaptureOutput inner = new CaptureOutput();
        //when
        final CapturedOutput outerCaptured = outer.of(() -> {
            final OngoingCapturedOutput innerCaptured = inner.ofThread(() -> {
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
    public void canRestoreNormalSystemErrWhenCapturingAsynchronously() throws Exception {
        //given
        final CaptureOutput outer = new CaptureOutput();
        final CaptureOutput inner = new CaptureOutput();
        //when
        final CapturedOutput outerCaptured = outer.of(() -> {
            final OngoingCapturedOutput innerCaptured = inner.ofThread(() -> {
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

    @Test
    public void canFlushCapturedOutputWhenCapturingAsynchronously() throws InterruptedException {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = captureOutput.ofThread(asyncRunnable);
        sleep(A_SHORT_PERIOD);
        ongoingCapturedOutput.flush();
        sleep(A_PERIOD);
        //then
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly("finished out");
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly("finished err");
    }

    @Test
    public void canCapturedOutputAndFlushWhenCapturingAsynchronously() throws InterruptedException {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = captureOutput.ofThread(asyncRunnable);
        sleep(A_SHORT_PERIOD);
        final CapturedOutput initialCapturedOutput = ongoingCapturedOutput.getCapturedOutputAndFlush();
        //then
        ongoingCapturedOutput.await(A_PERIOD, TimeUnit.MILLISECONDS);
        assertThat(initialCapturedOutput.getStdOut()).containsExactly("starting out");
        assertThat(initialCapturedOutput.getStdErr()).containsExactly("starting err");
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly("finished out");
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly("finished err");
    }

    @Test
    public void canWaitForThreadToComplete() {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        final CountDownLatch finishRunner = new CountDownLatch(1);
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = captureOutput.ofThread(finishRunner::await);
        //then
        assertThat(ongoingCapturedOutput.isRunning()).as("isRunning = true")
                                                     .isTrue();
        assertThat(ongoingCapturedOutput.isShutdown()).as("isShutdown = false")
                                                      .isFalse();
        finishRunner.countDown();
        ongoingCapturedOutput.await(A_PERIOD, TimeUnit.MILLISECONDS);
        assertThat(ongoingCapturedOutput.isRunning()).as("isRunning = false")
                                                     .isFalse();
        assertThat(ongoingCapturedOutput.isShutdown()).as("isShutdown = true")
                                                      .isTrue();
    }

    @Test
    public void interruptionDuringAsyncThreadSetupIsWrappedInOutputCaptureException() throws InterruptedException {
        //given
        final OutputCapturer captureOutput = new AbstractCaptureOutput() {

            @Override
            public CapturedOutput of(final ThrowingCallable callable) {
                return null;
            }

            @Override
            public CapturedOutput copyOf(final ThrowingCallable callable) {
                return null;
            }

            @Override
            public OngoingCapturedOutput ofThread(final ThrowingCallable callable) {
                return captureAsync(callable, router, latchFactory);
            }

            @Override
            public OngoingCapturedOutput copyOfThread(final ThrowingCallable callable) {
                return null;
            }
        };
        given(latchFactory.apply(1)).willReturn(latch);
        doThrow(InterruptedException.class).when(latch)
                                           .await();
        //when
        final ThrowableAssert.ThrowingCallable action = () -> {
            captureOutput.ofThread(asyncRunnable);
        };
        //then
        assertThatThrownBy(action).isInstanceOf(OutputCaptureException.class)
                                  .hasCauseInstanceOf(InterruptedException.class);
    }

    @Test
    public void interruptionDuringOngoingAwaitIsWrappedInOutputCaptureException() throws InterruptedException {
        //given
        final OngoingCapturedOutput ongoingCapturedOutput =
                new DefaultOngoingCapturedOutput(capturedOut, capturedErr, latch, thrownException);
        doThrow(InterruptedException.class).when(latch)
                                           .await(A_SHORT_PERIOD, TimeUnit.MILLISECONDS);
        //when
        final ThrowableAssert.ThrowingCallable action = () -> {
            ongoingCapturedOutput.await(A_SHORT_PERIOD, TimeUnit.MILLISECONDS);
        };
        //then
        assertThatThrownBy(action).isInstanceOf(OutputCaptureException.class)
                                  .hasCauseInstanceOf(InterruptedException.class);
    }

    @Test
    public void exceptionThrownInThreadIsAvailableToOngoingCapturedOutput() {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        final OutputCaptureException outputCaptureException = new OutputCaptureException("");
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = captureOutput.ofThread(() -> {
            throw outputCaptureException;
        });
        //then
        ongoingCapturedOutput.await(A_PERIOD, TimeUnit.MILLISECONDS);
        assertThat(ongoingCapturedOutput.thrownException()).contains(outputCaptureException);
    }

    @Test(timeout = 100_000L)
    public void canCaptureOutputAndCopyItToNormalOutputsWhenCapturingAsynchronously() {
        //given
        final CaptureOutput outer = new CaptureOutput();
        final CaptureOutput inner = new CaptureOutput();
        //when
        final CapturedOutput outerCaptured = outer.of(() -> {
            final OngoingCapturedOutput innerCaptured = inner.copyOfThread(() -> {
                System.out.println(line1);
            });
            innerCaptured.await(A_PERIOD, TimeUnit.MILLISECONDS);
            System.out.println(line2);
            assertThat(innerCaptured.getStdOut()).containsExactly(line1)
                                                 .doesNotContain(line2);
        });
        //then
        assertThat(outerCaptured.getStdOut()).containsExactly(line1, line2);
    }
}
