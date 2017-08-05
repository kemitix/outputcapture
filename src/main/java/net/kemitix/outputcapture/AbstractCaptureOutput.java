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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base for capturing output written to {@code System::out} and {@code System::err} as a {@link CapturedOutput}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
@SuppressWarnings({"classdataabstractioncoupling", "classfanoutcomplexity"})
abstract class AbstractCaptureOutput implements OutputCapturer {

    private static final AtomicInteger THREAD_GROUP_COUNTER = new AtomicInteger();

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
        final ThreadFactory threadFactory = r -> new Thread(createThreadGroup(), r);
        final ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        final AtomicReference<Exception> thrownException = new AtomicReference<>();
        final CountDownLatch finishedLatch = new CountDownLatch(1);
        final AtomicReference<CapturedPrintStream> capturedOut = new AtomicReference<>();
        final AtomicReference<CapturedPrintStream> capturedErr = new AtomicReference<>();
        executor.submit(initiateCapture(router, capturedOut, capturedErr));
        executor.submit(invokeCallable(callable, thrownException));
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

    private static ThreadGroup createThreadGroup() {
        return new ThreadGroup("CaptureOutput" + THREAD_GROUP_COUNTER.incrementAndGet());
    }

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
    protected OngoingCapturedOutput captureAsync(
            final ThrowingCallable callable, final Router router, final Function<Integer, CountDownLatch> latchFactory
                                                ) {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final AtomicReference<CapturedPrintStream> out = new AtomicReference<>();
        final AtomicReference<CapturedPrintStream> err = new AtomicReference<>();
        final CountDownLatch outputCapturedLatch = latchFactory.apply(1);
        final CountDownLatch completedLatch = latchFactory.apply(1);
        final AtomicReference<Exception> thrownException = new AtomicReference<>();
        executor.submit(initiateCapture(router, out, err));
        executor.submit(outputCapturedLatch::countDown);
        executor.submit(invokeCallable(callable, thrownException));
        executor.submit(shutdownAsyncCapture(out, err, completedLatch));
        executor.submit(executor::shutdown);
        awaitLatch(outputCapturedLatch);
        return new DefaultOngoingCapturedOutput(capturedTo(out), capturedTo(err), completedLatch, thrownException);
    }

    private ByteArrayOutputStream capturedTo(final AtomicReference<CapturedPrintStream> reference) {
        final CapturedPrintStream capturedPrintStream = reference.get();
        return capturedPrintStream.getCapturedTo();
    }

    private Runnable shutdownAsyncCapture(
            final AtomicReference<CapturedPrintStream> out, final AtomicReference<CapturedPrintStream> err,
            final CountDownLatch completedLatch
                                         ) {
        return () -> {
            System.setOut(originalStream(out));
            System.setErr(originalStream(err));
            completedLatch.countDown();
        };
    }

    private PrintStream originalStream(final AtomicReference<CapturedPrintStream> out) {
        final CapturedPrintStream capturedPrintStream = out.get();
        return capturedPrintStream.getOriginalStream();
    }

    @SuppressWarnings("illegalcatch")
    private Runnable invokeCallable(final ThrowingCallable callable, final AtomicReference<Exception> thrownException) {
        return () -> {
            try {
                callable.call();
            } catch (Exception e) {
                thrownException.set(e);
            }
        };
    }

    private Runnable initiateCapture(
            final Router router, final AtomicReference<CapturedPrintStream> out,
            final AtomicReference<CapturedPrintStream> err
                                    ) {
        return () -> {
            out.set(capturePrintStream(System.out, router, System::setOut));
            err.set(capturePrintStream(System.err, router, System::setErr));
        };
    }

    private void awaitLatch(final CountDownLatch outputCapturedLatch) {
        try {
            outputCapturedLatch.await();
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
