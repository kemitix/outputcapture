/*
  The MIT License (MIT)

  Copyright (c) 2018 Paul Campbell

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the "Software"), to deal in the Software without restriction,
  including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
  and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies
  or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
  AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.kemitix.outputcapture;

import lombok.val;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
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
     *
     * @return an instance of OngoingCapturedOutput
     */
    OngoingCapturedOutput capture(final ThrowingCallable callable) {
        val completedLatch = new CountDownLatch(1);
        val capturedOut = new ByteArrayOutputStream();
        val capturedErr = new ByteArrayOutputStream();
        val thrownExceptionReference = getThrownExceptionReference();
        final AtomicReference<OngoingCapturedOutput> capturedOutput = new AtomicReference<>();
        val executor = Executors.newSingleThreadExecutor();
        val started = new SafeLatch(1);
        executor.submit(() -> capturedOutput.set(
                new DefaultOngoingCapturedOutput(
                        capturedOut, capturedErr, completedLatch, thrownExceptionReference,
                        routerFactory.apply(RouterParameters.createDefault()))));
        executor.submit(started::countDown);
        executor.submit(() -> enable(capturedOutput.get()));
        executor.submit(() -> invokeCallable(callable));
        executor.submit(() -> disable(capturedOutput.get()));
        executor.submit(completedLatch::countDown);
        executor.submit(executor::shutdown);
        started.await();
        return capturedOutput.get();
    }

}
