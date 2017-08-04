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

import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base for capturing output written to {@code System::out} and {@code System::err} as a {@link CapturedOutput}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
abstract class AbstractCaptureOutput implements OutputCapturer {

    /**
     * Captures the output of the callable asynchronously.
     *
     * <p>This implementation launches the callable in a background thread then returns immediately.</p>
     *
     * @param callable     The Runnable to capture the output of
     * @param router       The Router to direct where written output is sent
     * @param latchFactory The Factory for creating CountDownLatch objects
     *
     * @return an instance of OngoingCapturedOutput
     */
    //FIXME: reduce complexity
    @SuppressWarnings({"npathcomplexity", "illegalcatch"})
    protected OngoingCapturedOutput captureAsync(final ThrowingCallable callable, final Router router, final
                                                 Function<Integer, CountDownLatch> latchFactory
                                                 ) {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final AtomicReference<CapturedPrintStream> out = new AtomicReference<>();
        final AtomicReference<CapturedPrintStream> err = new AtomicReference<>();
        final CountDownLatch outputCapturedLatch = latchFactory.apply(1);
        executor.submit(() -> {
            out.set(capturePrintStream(System.out, router, System::setOut));
            err.set(capturePrintStream(System.err, router, System::setErr));
            outputCapturedLatch.countDown();
        });
        final AtomicReference<Throwable> thrownException = new AtomicReference<>();
        executor.submit(() -> {
            try {
                callable.call();
            } catch (Exception e) {
                thrownException.set(e);
            }
        });
        executor.submit(() -> {
            System.setOut(out.get()
                             .getOriginalStream());
            System.setErr(err.get()
                             .getOriginalStream());
            executor.shutdown();
        });
        try {
            outputCapturedLatch.await();
        } catch (InterruptedException e) {
            throw new OutputCaptureException("Error initiating capture", e);
        }
        return new DefaultOngoingCapturedOutput(out.get()
                                                   .getCapturedTo(), err.get()
                                                                        .getCapturedTo(), executor, thrownException);
    }

    /**
     * Captures the output of the callable then returns.
     *
     * @param callable The callable to capture the output of
     * @param router   The Router to direct where written output is sent
     *
     * @return an instance of CapturedOutput
     */
    @SuppressWarnings("illegalcatch")
    protected CapturedOutput capture(
            final ThrowingCallable callable, final Router router
                                    ) {
        final CapturedPrintStream capturedOut = capturePrintStream(System.out, router, System::setOut);
        final CapturedPrintStream capturedErr = capturePrintStream(System.err, router, System::setErr);
        try {
            callable.call();
            if (System.out != capturedOut.getReplacementStream()) {
                throw new OutputCaptureException("System.out has been replaced");
            }
        } catch (OutputCaptureException e) {
            throw e;
        } catch (Exception e) {
            throw new OutputCaptureException(e);
        } finally {
            System.setOut(capturedOut.getOriginalStream());
            System.setErr(capturedErr.getOriginalStream());
        }
        return new DefaultCapturedOutput(capturedOut.getCapturedTo(), capturedErr.getCapturedTo());
    }

    private CapturedPrintStream capturePrintStream(
            final PrintStream originalStream, final Router router, final Consumer<PrintStream> setStream
                                                  ) {
        final CapturedPrintStream capturedPrintStream = new CapturedPrintStream(originalStream, router);
        setStream.accept(capturedPrintStream.getReplacementStream());
        return capturedPrintStream;
    }
}
