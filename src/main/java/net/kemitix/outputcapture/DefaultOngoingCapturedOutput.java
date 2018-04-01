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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The captured output being written to System.out and System.err.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class DefaultOngoingCapturedOutput extends DefaultCapturedOutput implements OngoingCapturedOutput {

    private final CountDownLatch completedLatch;

    private final AtomicReference<Exception> thrownException;

    /**
     * Constructor.
     *
     * @param capturedOut     The captured output written to System.out
     * @param capturedErr     The captured output written to System.err
     * @param completedLatch  The Latch indicating the thread is still running
     * @param thrownException The reference to any exception thrown
     */
    DefaultOngoingCapturedOutput(
            final ByteArrayOutputStream capturedOut, final ByteArrayOutputStream capturedErr,
            final CountDownLatch completedLatch, final AtomicReference<Exception> thrownException
    ) {
        super(capturedOut, capturedErr);
        this.completedLatch = completedLatch;
        this.thrownException = thrownException;
    }

    @Override
    public CapturedOutput getCapturedOutputAndFlush() {
        final List<String> collectedOut = asStream(getCapturedOut()).collect(Collectors.toList());
        final List<String> collectedErr = asStream(getCapturedErr()).collect(Collectors.toList());
        flush();
        return new CapturedOutput() {

            @Override
            public Stream<String> getStdOut() {
                return collectedOut.stream();
            }

            @Override
            public Stream<String> getStdErr() {
                return collectedErr.stream();
            }

            @Override
            public ByteArrayOutputStream out() {
                return null;
            }

            @Override
            public ByteArrayOutputStream err() {
                return null;
            }
        };
    }

    @Override
    public void flush() {
        out().reset();
        err().reset();
    }

    @Override
    public boolean isRunning() {
        return !isShutdown();
    }

    @Override
    public boolean isShutdown() {
        return completedLatch.getCount() == 0;
    }

    @Override
    public void await(final long timeout, final TimeUnit unit) {
        try {
            completedLatch.await(timeout, unit);
        } catch (InterruptedException e) {
            throw new OutputCaptureException("Error awaiting termination", e);
        }
    }

    @Override
    public Optional<Throwable> thrownException() {
        return Optional.ofNullable(thrownException.get());
    }
}
