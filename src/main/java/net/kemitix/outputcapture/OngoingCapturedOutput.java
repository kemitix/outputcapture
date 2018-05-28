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

import java.util.Optional;

/**
 * The output that is being written to {@code System.out} and {@code System.err}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public interface OngoingCapturedOutput extends RoutableCapturedOutput {

    /**
     * Get the captured output to System.out and System.err collected so far and start again.
     *
     * <p>Subsequent calls to {@link #getStdOut()}, {@link #getStdErr()} or {@code getCapturedOutputAndFlush()} will
     * only return output captured since this call.</p>
     *
     * @return a Stream of Strings, one line per String using the system's line separator
     */
    CapturedOutput getCapturedOutputAndFlush();

    /**
     * Discard all captured output so far and continue capturing from this point.
     */
    void flush();

    /**
     * Returns an optional containing any exception that was thrown by the captured task.
     *
     * @return an Optional containing an exception, or empty if none was thrown
     */
    Optional<Throwable> thrownException();

    /**
     * Checks if the ExecutorService has been shutdown yet.
     *
     * @return true if the ExecutorService has been shutdown.
     */
    boolean executorIsShutdown();

    /**
     * Blocks until the async thread completed.
     */
    void join();
}
