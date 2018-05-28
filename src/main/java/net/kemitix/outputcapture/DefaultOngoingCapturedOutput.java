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

import lombok.Getter;
import lombok.val;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The captured output being written to System.out and System.err.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class DefaultOngoingCapturedOutput extends DefaultCapturedOutput implements OngoingCapturedOutput {

    @Getter
    private final SafeLatch completedLatch;

    private final AtomicReference<Exception> thrownException;
    private final ExecutorService executor;

    private final Function<ByteArrayOutputStream, ByteArrayOutputStream> streamCopy = new StreamCopyFunction();

    /**
     * Constructor.
     *
     * @param capturedOut     The captured output written to System.out
     * @param capturedErr     The captured output written to System.err
     * @param completedLatch  The Latch indicating the thread is still running
     * @param thrownException The reference to any exception thrown
     * @param router          The router to direct the output
     * @param executor        The executor service
     * @param capturedLines   The captured lines
     */
    DefaultOngoingCapturedOutput(
            final ByteArrayOutputStream capturedOut,
            final ByteArrayOutputStream capturedErr,
            final SafeLatch completedLatch,
            final AtomicReference<Exception> thrownException,
            final Router router,
            final ExecutorService executor,
            final CapturedLines capturedLines
    ) {
        super(capturedOut, capturedErr, router, capturedLines);
        this.completedLatch = completedLatch;
        this.thrownException = thrownException;
        this.executor = executor;
    }

    @Override
    public CapturedOutput getCapturedOutputAndFlush() {
        val capturedOutput = new DefaultCapturedOutput(copyOf(out()), copyOf(err()), getRouter(), getCapturedLines());
        flush();
        return capturedOutput;
    }

    private ByteArrayOutputStream copyOf(final ByteArrayOutputStream outputStream) {
        return streamCopy.apply(outputStream);
    }

    @Override
    public void flush() {
        out().reset();
        err().reset();
    }

    @Override
    public Optional<Throwable> thrownException() {
        return Optional.ofNullable(thrownException.get());
    }

    @Override
    public boolean executorIsShutdown() {
        return executor.isShutdown();
    }

    @Override
    public void join() {
        getCompletedLatch().await();
    }

    /**
     * Fetch all the captured lines as a stream.
     *
     * <p>This implementation waits until the ongoing capture completes before returning the stream.</p>
     *
     * @return a Stream of CapturedOutputLines
     */
    @Override
    public Stream<CapturedOutputLine> stream() {
        join();
        return super.stream();
    }
}
