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

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Capture the output of a callable, then return the captured output.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 *
 * @see AsynchronousOutputCapturer
 */
class SynchronousOutputCapturer extends AbstractCaptureOutput {

    private final Router router;

    /**
     * Constructor.
     *
     * @param router   The Router to direct where written output is sent
     */
    SynchronousOutputCapturer(final Router router) {
        this.router = router;
    }

    /**
     * Captures the output of the callable then returns.
     *
     * @param callable The callable to capture the output of
     *
     * @return an instance of CapturedOutput
     */
    protected CapturedOutput capture(final ThrowingCallable callable) {
        final ThreadFactory threadFactory = r -> new Thread(createThreadGroup(), r);
        final ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        final AtomicReference<Exception> thrownException = new AtomicReference<>();
        final CountDownLatch finishedLatch = new CountDownLatch(1);
        final AtomicReference<CapturedPrintStream> capturedOut = new AtomicReference<>();
        final AtomicReference<CapturedPrintStream> capturedErr = new AtomicReference<>();
        executor.submit(() -> initiateCapture(router, capturedOut, capturedErr));
        executor.submit(() -> invokeCallable(callable, thrownException));
        executor.submit(finishedLatch::countDown);
        executor.submit(executor::shutdown);
        awaitLatch(finishedLatch);
        if (System.out != capturedOut.get()
                                     .getReplacementStream()) {
            throw new OutputCaptureException("System.out has been replaced");
        }
        System.setOut(originalStream(capturedOut));
        System.setErr(originalStream(capturedErr));
        if (Optional.ofNullable(thrownException.get())
                    .isPresent()) {
            throw new OutputCaptureException(thrownException.get());
        }
        return new DefaultCapturedOutput(capturedTo(capturedOut), capturedTo(capturedErr));
    }
}
