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
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Capture the output of a callable, then return the captured output.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 *
 * @see AsynchronousOutputCapturer
 */
class SynchronousOutputCapturer extends AbstractCaptureOutput {

    private final Function<RouterParameters, Router> routerFactory;

    /**
     * Constructor.
     *
     * @param routerFactory   The Router to direct where written output is sent
     */
    SynchronousOutputCapturer(final Function<RouterParameters, Router> routerFactory) {
        this.routerFactory = routerFactory;
    }

    /**
     * Captures the output of the callable then returns.
     *
     * @param callable The callable to capture the output of
     * @param maxAwaitMilliseconds the maximum number of milliseconds to await for the capture to complete
     *
     * @return an instance of CapturedOutput
     */
    CapturedOutput capture(final ThrowingCallable callable, final Long maxAwaitMilliseconds) {
        val executor = Executors.newSingleThreadExecutor();
        val finished = new SafeLatch(1, maxAwaitMilliseconds, executor::shutdown);
        val capturedOutput = new AtomicReference<RoutableCapturedOutput>();
        executeAsync(executor, callable, finished, capturedOutput);
        finished.awaitThen(() -> disable(capturedOutput.get()));
        throwAnyExceptions();
        return capturedOutput.get();
    }

    private void executeAsync(
            final ExecutorService executor,
            final ThrowingCallable callable,
            final SafeLatch finished,
            final AtomicReference<RoutableCapturedOutput> capturedOutput
    ) {
        executor.submit(() -> capturedOutput.set(outputCaptor()));
        executor.submit(() -> enable(capturedOutput.get()));
        executor.submit(() -> invokeCallable(callable));
        executor.submit(finished::countDown);
        executor.submit(executor::shutdown);
    }

    private void throwAnyExceptions() {
        val exception = getThrownExceptionReference().get();
        Optional.ofNullable(exception)
                .ifPresent(e -> {
                    throw new OutputCaptureException(e);
                });
    }

    private RoutableCapturedOutput outputCaptor() {
        val capturedOut = new ByteArrayOutputStream();
        val capturedErr = new ByteArrayOutputStream();
        val routerParameters = RouterParameters.createDefault();
        val router = routerFactory.apply(routerParameters);
        return new DefaultCapturedOutput(
                capturedOut,
                capturedErr,
                router
        );
    }

}
