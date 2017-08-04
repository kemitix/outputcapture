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

/**
 * Base for capturing output written to {@code System::out} and {@code System::err} as a {@link CapturedOutput}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
abstract class AbstractCaptureOutput implements OutputCapturer {

    /**
     * Captures the output of the runnable asynchronously.
     *
     * <p>This implementation launches the runnable in a background thread then returns immediately.</p>
     *
     * @param runnable The Runnable to capture the output of
     * @param router   The Router to direct where written output is sent
     *
     * @return an instance of OngoingCapturedOutput
     *
     * @throws InterruptedException if there is an error while waiting for the outputs to be redirected
     */
    protected OngoingCapturedOutput captureAsync(final Runnable runnable, final Router router)
            throws InterruptedException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final AtomicReference<CapturedPrintStream> out = new AtomicReference<>();
        final AtomicReference<CapturedPrintStream> err = new AtomicReference<>();
        final CountDownLatch outputCapturedDone = new CountDownLatch(1);
        executor.submit(() -> {
            out.set(capturePrintStream(System.out, router, System::setOut));
            err.set(capturePrintStream(System.err, router, System::setErr));
            outputCapturedDone.countDown();
        });
        executor.submit(runnable);
        executor.submit(() -> {
            System.setOut(out.get()
                             .getReplacementStream());
            System.setErr(err.get()
                             .getReplacementStream());
        });
        outputCapturedDone.await();
        return new DefaultOngoingCapturedOutput(out.get()
                                                   .getCapturedTo(), err.get()
                                                                        .getCapturedTo());
    }

    /**
     * Captures the output of the runnable then returns.
     *
     * @param runnable The Runnable to capture the output of
     * @param router   The Router to direct where written output is sent
     *
     * @return an instance of CapturedOutput
     */
    protected CapturedOutput capture(
            final Runnable runnable, final Router router
                                    ) {
        final CapturedPrintStream capturedOut = capturePrintStream(System.out, router, System::setOut);
        final CapturedPrintStream capturedErr = capturePrintStream(System.err, router, System::setErr);
        runnable.run();
        if (System.out != capturedOut.getReplacementStream()) {
            throw new OutputCaptureException("System.out has been replaced");
        }
        System.setOut(capturedOut.getOriginalStream());
        System.setErr(capturedErr.getOriginalStream());
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
