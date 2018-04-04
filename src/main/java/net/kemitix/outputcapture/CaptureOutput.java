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

import java.util.concurrent.CountDownLatch;

/**
 * Captures the output written to standard out and standard error.
 *
 * <table>
 *     <thead><tr><th>method</th><th>a/sync</th><th>filtering</th><th>redirect/copy</th></tr></thead>
 *     <tbody>
 *         <tr><td>of</td><td>sync</td><td>thread</td><td>redirect</td></tr>
 *         <tr><td>copyOf</td><td>sync</td><td>thread</td><td>copy</td></tr>
 *         <tr><td>ofThread</td><td>async</td><td>thread</td><td>redirect</td></tr>
 *         <tr><td>copyOfThread</td><td>async</td><td>thread</td><td>copy</td></tr>
 *         <tr><td>?</td><td>sync</td><td>any</td><td>redirect</td></tr>
 *         <tr><td>?</td><td>sync</td><td>any</td><td>copy</td></tr>
 *         <tr><td>whileDoing</td><td>async</td><td>any</td><td>redirect</td></tr>
 *         <tr><td>copyWhileDoing</td><td>async</td><td>any</td><td>copy</td></tr>
 *     </tbody>
 * </table>
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public interface CaptureOutput {

    /**
     * Capture the output of the callable.
     *
     * @param callable the callable to capture the output of
     *
     * @return the instance CapturedOutput
     */
    static CapturedOutput of(ThrowingCallable callable) {
        return new SynchronousOutputCapturer(ThreadFilteredRedirectRouter::new)
                .capture(callable);
    }

    /**
     * Capture the output of the callable and copies to normal output.
     *
     * @param callable the callable to capture the output of
     *
     * @return the instance CapturedOutput
     */
    static CapturedOutput copyOf(ThrowingCallable callable) {
        return new SynchronousOutputCapturer(ThreadFilteredCopyRouter::new)
                .capture(callable);
    }

    /**
     * Capture the output of a running thread asynchronously.
     *
     * <p>{@code callable} is started in a new thread.</p>
     *
     * @param callable the callable to capture the output of
     *
     * @return an instance of OngoingCapturedOutput
     */
    static OngoingCapturedOutput ofThread(ThrowingCallable callable) {
        return new AsynchronousOutputCapturer(ThreadFilteredRedirectRouter::new)
                .capture(callable, CountDownLatch::new);
    }

    /**
     * Capture the output of the callable running asynchronously and copies to the normal output.
     *
     * <p>{@code callable} is started in a new thread.</p>
     *
     * @param callable the callable to capture the output of
     *
     * @return an instance of OngoingCapturedOutput
     */
    static OngoingCapturedOutput copyOfThread(ThrowingCallable callable) {
        return new AsynchronousOutputCapturer(ThreadFilteredCopyRouter::new)
                .capture(callable, CountDownLatch::new);
    }

    /**
     * Capture all output written while the callable is running.
     *
     * <p>This method will also capture any other output from other threads during the time the callable is running.</p>
     *
     * @param callable the callable to capture output during
     *
     * @return an instance of CapturedOutput
     */
    static OngoingCapturedOutput whileDoing(ThrowingCallable callable) {
        return new AsynchronousOutputCapturer(PromiscuousRedirectRouter::new)
                .capture(callable, CountDownLatch::new);
    }

    /**
     * Capture all output written while the callable is running.
     *
     * <p>This method will also capture any other output from other threads during the time the callable is
     * running.</p>
     *
     * <p>{@code callable} is started in a new thread.</p>
     *
     * @param callable the callable to capture output during
     *
     * @return an instance of OngoingCapturedOutput
     */
    static OngoingCapturedOutput copyWhileDoing(ThrowingCallable callable) {
        return new AsynchronousOutputCapturer(PromiscuousCopyRouter::new)
                .capture(callable, CountDownLatch::new);
    }

    /**
     * The number of active captures in place.
     *
     * @return a count the {@code OutputCapturer} instances in effect
     */
    static int activeCount() {
        return AbstractCaptureOutput.activeCount();
    }
}
