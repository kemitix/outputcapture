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

import java.util.concurrent.Executors;

/**
 * Factories for creating {@link CaptureOutput} implementations.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
interface Captors {

    /**
     * Create an {@link CaptureOutput} instance that will intercept and capture output synchronously for a single
     * thread.
     *
     * @return A redirecting and synchronous CaptureOutput tied to a single thread
     */
    static SynchronousOutputCapturer syncRedirectThread() {
        return new SynchronousOutputCapturer(ThreadFilteredRedirectRouter::new);
    }

    /**
     * Create an {@link CaptureOutput} instance that will capture output synchronously for a single thread.
     *
     * @return A copying and synchronous CaptureOutput tied to a single thread
     */
    static SynchronousOutputCapturer syncCopy() {
        return new SynchronousOutputCapturer(ThreadFilteredCopyRouter::new);
    }

    /**
     * Create an {@link CaptureOutput} instance that will intercept and capture output synchronously from all threads.
     *
     * @return A redirecting and synchronous CaptureOutput
     */
    static SynchronousOutputCapturer syncRedirectAll() {
        return new SynchronousOutputCapturer(
                routerParameters -> new PromiscuousRedirectRouter(routerParameters.getCapturedLines()));
    }

    /**
     * Create an {@link CaptureOutput} instance that will intercept and capture output asynchronously for a single
     * thread.
     *
     * @param maxAwaitMilliseconds the maximum number of milliseconds to await for the capture to complete
     * @return A redirecting and asynchronous CaptureOutput tied to a single thread
     */
    static AsynchronousOutputCapturer asyncRedirectThread(final Long maxAwaitMilliseconds) {
        return new AsynchronousOutputCapturer(ThreadFilteredRedirectRouter::new, maxAwaitMilliseconds,
                Executors.newSingleThreadExecutor());
    }

    /**
     * Create an {@link CaptureOutput} instance that will capture output asynchronously for a single thread.
     *
     * @param maxAwaitMilliseconds the maximum number of milliseconds to await for the capture to complete
     * @return A copying and asynchronous CaptureOutput tied to a single thread
     */
    static AsynchronousOutputCapturer asyncCopyThread(final Long maxAwaitMilliseconds) {
        return new AsynchronousOutputCapturer(ThreadFilteredCopyRouter::new, maxAwaitMilliseconds,
                Executors.newSingleThreadExecutor());
    }

    /**
     * Create an {@link CaptureOutput} instance that will intercept and capture output asynchronously from all threads.
     *
     * @param maxAwaitMilliseconds the maximum number of milliseconds to await for the capture to complete
     * @return A redirecting and asynchronous CaptureOutput
     */
    static AsynchronousOutputCapturer asyncRedirectAll(final Long maxAwaitMilliseconds) {
        return new AsynchronousOutputCapturer(
                routerParameters -> new PromiscuousRedirectRouter(routerParameters.getCapturedLines()),
                maxAwaitMilliseconds,
                Executors.newSingleThreadExecutor());
    }

    /**
     * Create an {@link CaptureOutput} instance that will capture output asynchronously from all threads.
     *
     * @param maxAwaitMilliseconds the maximum number of milliseconds to await for the capture to complete
     * @return A copying and asynchronous CaptureOutput
     */
    static AsynchronousOutputCapturer asyncCopyAll(final Long maxAwaitMilliseconds) {
        return new AsynchronousOutputCapturer(
                routerParameters -> new PromiscuousCopyRouter(routerParameters.getCapturedLines()),
                maxAwaitMilliseconds,
                Executors.newSingleThreadExecutor());
    }
}
