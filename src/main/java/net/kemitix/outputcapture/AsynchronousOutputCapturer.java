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

    private final Function<RouterParameters, Router> routerFactory;

    /**
     * Constructor.
     *
     * @param routerFactory The Router to direct where written output is sent
     */
    AsynchronousOutputCapturer(final Function<RouterParameters, Router> routerFactory) {
        this.routerFactory = routerFactory;
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
        final CountDownLatch completedLatch = latchFactory.apply(1);
        final OngoingCapturedOutput captureOutput =
                new DefaultOngoingCapturedOutput(
                        new ByteArrayOutputStream(),
                        new ByteArrayOutputStream(),
                        completedLatch,
                        getThrownException()
                );
        invoke(captureOutput, callable, completedLatch);
        return captureOutput;
    }

    private void invoke(
            final OngoingCapturedOutput captureOutput,
            final ThrowingCallable callable,
            final CountDownLatch completedLatch
    ) {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            enable(captureOutput, routerFactory.apply(RouterParameters.createDefault()));
            invokeCallable(callable);
            disable(captureOutput);
            completedLatch.countDown();
            executor.shutdown();
        });
    }
}
