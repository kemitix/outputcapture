/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Paul Campbell
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.kemitix.outputcapture;

import lombok.AccessLevel;
import lombok.Getter;
import net.kemitix.wrapper.Wrapper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Base for capturing output written to {@code System::out} and {@code System::err} as a {@link CapturedOutput}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
abstract class AbstractCaptureOutput {

    @Getter(AccessLevel.PROTECTED)
    private CapturedPrintStream capturedOut;

    @Getter(AccessLevel.PROTECTED)
    private CapturedPrintStream capturedErr;

    @Getter(AccessLevel.PROTECTED)
    private AtomicReference<Exception> thrownException = new AtomicReference<>();

    /**
     * Get the backing byte array from the CapturedPrintStream.
     *
     * @param capturedPrintStream The CapturedPrintStream containing the backing byte array
     *
     * @return the backing byte array
     */
    protected ByteArrayOutputStream capturedTo(final CapturedPrintStream capturedPrintStream) {
        return capturedPrintStream.getCapturedTo();
    }

    /**
     * Restore the original PrintStreams for System.out and System.err.
     *
     * @param out            The CapturedPrintStream containing the original System.out
     * @param err            The CapturedPrintStream containing the original System.err
     * @param completedLatch The latch to release once the PrintStreams are restored
     */
    protected void shutdownAsyncCapture(
            final CapturedPrintStream out, final CapturedPrintStream err, final CountDownLatch completedLatch
                                       ) {
        stop(out, err);
        completedLatch.countDown();
    }

    /**
     * Invokes the Callable and stores any thrown exception.
     *
     * @param callable The callable to invoke
     */
    @SuppressWarnings("illegalcatch")
    protected void invokeCallable(final ThrowingCallable callable) {
        try {
            callable.call();
        } catch (Exception e) {
            thrownException.set(e);
        }
    }

    /**
     * Captures the System.out and System.err PrintStreams.
     *
     * @param router       The Router
     * @param targetThread The target thread to filter by
     */
    protected void initiateCapture(final Router router, final Thread targetThread) {
        capturedOut = capturePrintStream(System.out, router, System::setOut, targetThread);
        capturedErr = capturePrintStream(System.err, router, System::setErr, targetThread);
    }

    /**
     * Wait for the CountDownLatch to count down to 0.
     *
     * <p>Any {@link InterruptedException} that is thrown will be wrapped in an {@link OutputCaptureException}.</p>
     *
     * @param countDownLatch The latch to wait on
     */
    protected void awaitLatch(final CountDownLatch countDownLatch) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new OutputCaptureException("Error awaiting latch", e);
        }
    }

    private CapturedPrintStream capturePrintStream(
            final PrintStream originalStream, final Router router, final Consumer<PrintStream> setStream,
            final Thread targetThread
                                                  ) {
        final CapturedPrintStream capturedPrintStream = new CapturedPrintStream(originalStream, router, targetThread);
        final Wrapper<PrintStream> replacementStream = capturedPrintStream.getReplacementStream();
        setStream.accept(replacementStream.asCore());
        return capturedPrintStream;
    }

    /**
     * Stop capturing the System PrintStreams {@code out} and {@code err}.
     *
     * @param capturedPrintStreamOut The {@code System.out} captured PrintStream
     * @param capturedPrintStreamErr The {@code System.err} captured PrintStream
     */
    protected void stop(
            final CapturedPrintStream capturedPrintStreamOut, final CapturedPrintStream capturedPrintStreamErr
                       ) {
        stopCapturing(System.out, System::setOut, capturedPrintStreamOut);
        stopCapturing(System.err, System::setErr, capturedPrintStreamErr);
    }

    @SuppressWarnings("unchecked")
    private void stopCapturing(
            final PrintStream current, final Consumer<PrintStream> setPrintStream, final CapturedPrintStream captured
                              ) {
        final AtomicReference<PrintStream> head = new AtomicReference<>(current);
        captured.getWrappers()
                .forEach(wrapperToRemove -> Wrapper.asWrapper(head.get())
                                                   .ifPresent(wrapper -> head.set(
                                                           wrapper.removeWrapper(wrapperToRemove))));
        setPrintStream.accept(head.get());
    }
}
