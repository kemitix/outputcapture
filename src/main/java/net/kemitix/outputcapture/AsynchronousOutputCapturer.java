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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Run a callable in a new thread and capture its output.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 *
 * @see SynchronousOutputCapturer
 */
class AsynchronousOutputCapturer extends AbstractCaptureOutput {

    private final Router router;

    /**
     * Constructor.
     *
     * @param router The Router to direct where written output is sent
     */
    AsynchronousOutputCapturer(final Router router) {
        this.router = router;
    }

    /**
     * Captures the output of the callable asynchronously.
     *
     * <p>This implementation launches the callable in a background thread then returns immediately.</p>
     *
     * @param callable     The Runnable to capture the output of
     * @param latchFactory The Factory for creating CountDownLatch objects
     *
     * @return an instance of OngoingCapturedOutput
     */
    protected OngoingCapturedOutput capture(
            final ThrowingCallable callable, final Function<Integer, CountDownLatch> latchFactory
                                           ) {
        final CountDownLatch outputCapturedLatch = latchFactory.apply(1);
        final CountDownLatch completedLatch = latchFactory.apply(1);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> initiateCapture(router, Thread.currentThread()));
        executor.submit(outputCapturedLatch::countDown);
        executor.submit(() -> invokeCallable(callable));
        executor.submit(() -> shutdownAsyncCapture(getCapturedOut(), getCapturedErr(), completedLatch));
        executor.submit(executor::shutdown);
        awaitLatch(outputCapturedLatch);
        return new DefaultOngoingCapturedOutput(
                capturedTo(getCapturedOut()), capturedTo(getCapturedErr()), completedLatch, getThrownException());
    }
}
