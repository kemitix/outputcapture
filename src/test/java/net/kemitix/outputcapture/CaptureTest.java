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

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for capturing output.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class CaptureTest {

    private static final long A_PERIOD = 200L;

    private static final long A_SHORT_PERIOD = 100L;

    private String line1;

    private String line2;

    private Runnable asyncRunnable;

    @Before
    public void setUp() throws Exception {
        line1 = randomText();
        line2 = randomText();
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
    public void onlyCapturesOutputFromTargetRunnable() {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        final Runnable runnable = () -> {
            System.out.println("started");
            sleep(A_PERIOD);
            System.out.println("finished");
        };
        new Thread(() -> {
            sleep(A_SHORT_PERIOD);
            System.out.println("ignore me");
        }).start();
        //when
        final CapturedOutput capturedOutput = captureOutput.of(runnable);
        //then
        assertThat(capturedOutput.getStdOut()).containsExactly("started", "finished");
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
    public void ignoresOutputFromOtherThreads() {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        //when
        final CapturedOutput capturedOutput = captureOutput.of(() -> {
            runOnThreadAndWait(() -> {
                System.out.println("message");
                System.out.write('x');
            });
        });
        //then
        assertThat(capturedOutput.getStdOut()).containsExactly("");
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
        final CaptureOutput captureOutput = new CaptureOutput();
        final int activeCount = Thread.activeCount();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = captureOutput.ofThread(asyncRunnable);
        //then
        sleep(A_SHORT_PERIOD);
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly("starting out");
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly("starting err");
        sleep(A_PERIOD);
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly("starting out", "finished out");
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly("starting err", "finished err");
        assertThat(Thread.activeCount()).as("remove thread")
                                        .isEqualTo(activeCount);
    }

    @Test
    public void canFlushCapturedOutputWhenCapturingAsynchronously() throws InterruptedException {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        final int activeCount = Thread.activeCount();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = captureOutput.ofThread(asyncRunnable);
        sleep(A_SHORT_PERIOD);
        ongoingCapturedOutput.flush();
        sleep(A_PERIOD);
        //then
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly("finished out");
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly("finished err");
        assertThat(Thread.activeCount()).as("remove thread")
                                        .isEqualTo(activeCount);
    }

    @Test
    public void canCapturedOutputAndFlushWhenCapturingAsynchronously() throws InterruptedException {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        final int activeCount = Thread.activeCount();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = captureOutput.ofThread(asyncRunnable);
        sleep(A_SHORT_PERIOD);
        final CapturedOutput initialCapturedOutput = ongoingCapturedOutput.getCapturedOutputAndFlush();
        sleep(A_PERIOD);
        //then
        assertThat(initialCapturedOutput.getStdOut()).containsExactly("starting out");
        assertThat(initialCapturedOutput.getStdErr()).containsExactly("starting err");
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly("finished out");
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly("finished err");
        assertThat(Thread.activeCount()).as("remove thread")
                                        .isEqualTo(activeCount);
    }

    @Test
    public void canWaitForThreadToComplete() {
        //given
        final CaptureOutput captureOutput = new CaptureOutput();
        final int activeCount = Thread.activeCount();
        final CountDownLatch finishRunner = new CountDownLatch(1);
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = captureOutput.ofThread(() -> {
            try {
                finishRunner.await();
            } catch (InterruptedException e) {
                fail("error waiting");
            }
        });
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
        assertThat(Thread.activeCount()).as("remove thread")
                                        .isEqualTo(activeCount);
    }
}
