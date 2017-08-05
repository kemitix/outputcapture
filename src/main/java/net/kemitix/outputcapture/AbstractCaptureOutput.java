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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Base for capturing output written to {@code System::out} and {@code System::err} as a {@link CapturedOutput}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
abstract class AbstractCaptureOutput {

    private static final AtomicInteger THREAD_GROUP_COUNTER = new AtomicInteger();

    /**
     * Create a new ThreadGroup.
     *
     * @return a new ThreadGroup
     */
    protected static ThreadGroup createThreadGroup() {
        return new ThreadGroup("CaptureOutput" + THREAD_GROUP_COUNTER.incrementAndGet());
    }

    /**
     * Get the backing byte array from the CapturedPrintStream reference.
     *
     * @param reference The reference to the CapturedPrintStream containing the backing byte array
     *
     * @return the backing byte array
     */
    protected ByteArrayOutputStream capturedTo(final AtomicReference<CapturedPrintStream> reference) {
        final CapturedPrintStream capturedPrintStream = reference.get();
        return capturedPrintStream.getCapturedTo();
    }

    /**
     * Restore the original PrintStreams for System.out and System.err.
     *
     * @param out            The reference to the CapturedPrintStream containing the original System.out
     * @param err            The reference to the CapturedPrintStream containing the original System.err
     * @param completedLatch The latch to release once the PrintStreams are restored
     */
    protected void shutdownAsyncCapture(
            final AtomicReference<CapturedPrintStream> out, final AtomicReference<CapturedPrintStream> err,
            final CountDownLatch completedLatch
                                       ) {
        System.setOut(originalStream(out));
        System.setErr(originalStream(err));
        completedLatch.countDown();
    }

    /**
     * Get the original PrintStream from the CapturedPrintStream reference.
     *
     * @param reference The reference to the CapturedPrintStream containing the original PrintStream
     *
     * @return the original PrintStream
     */
    protected PrintStream originalStream(final AtomicReference<CapturedPrintStream> reference) {
        final CapturedPrintStream capturedPrintStream = reference.get();
        return capturedPrintStream.getOriginalStream();
    }

    /**
     * Invokes the Callable and stores any thrown exception.
     *
     * @param callable        The callable to invoke
     * @param thrownException The output parameter references any exception thrown by callable
     */
    @SuppressWarnings("illegalcatch")
    protected void invokeCallable(
            final ThrowingCallable callable, final AtomicReference<Exception> thrownException
                                 ) {
        try {
            callable.call();
        } catch (Exception e) {
            thrownException.set(e);
        }
    }

    /**
     * Captures the System.out and System.err PrintStreams.
     *
     * @param router The Router
     * @param out    The output parameter referencing the CapturedPrintStream for System.out
     * @param err    The output parameter referencing the CapturedPrintStream for System.err
     */
    protected void initiateCapture(
            final Router router, final AtomicReference<CapturedPrintStream> out,
            final AtomicReference<CapturedPrintStream> err
                                  ) {
        out.set(capturePrintStream(System.out, router, System::setOut));
        err.set(capturePrintStream(System.err, router, System::setErr));
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
            final PrintStream originalStream, final Router router, final Consumer<PrintStream> setStream
                                                  ) {
        final CapturedPrintStream capturedPrintStream = new CapturedPrintStream(originalStream, router);
        setStream.accept(capturedPrintStream.getReplacementStream());
        return capturedPrintStream;
    }
}
