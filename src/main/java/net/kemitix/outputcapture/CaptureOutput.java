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

import java.util.concurrent.CountDownLatch;

/**
 * Captures output written to {@code System::out} and {@code System::err} as a {@link CapturedOutput}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public final class CaptureOutput implements OutputCapturer {

    private static final WrapperFactory WRAPPER_FACTORY = new WrapperFactoryImpl();

    private static final Router THREAD_FILTERED_REDIRECT_ROUTER = new ThreadFilteredRedirectRouter(WRAPPER_FACTORY);

    private static final Router THREAD_FILTERED_COPY_ROUTER = new ThreadFilteredCopyRouter(WRAPPER_FACTORY);

    private static final Router COPY_ROUTER = new CopyRouter(WRAPPER_FACTORY);

    private static final Router REDIRECT_ROUTER = new RedirectRouter(WRAPPER_FACTORY);

    @Override
    public CapturedOutput of(final ThrowingCallable callable) {
        return new SynchronousOutputCapturer(THREAD_FILTERED_REDIRECT_ROUTER).capture(callable);
    }

    @Override
    public CapturedOutput copyOf(final ThrowingCallable callable) {
        return new SynchronousOutputCapturer(THREAD_FILTERED_COPY_ROUTER).capture(callable);
    }

    @Override
    public OngoingCapturedOutput ofThread(final ThrowingCallable callable) {
        return new AsynchronousOutputCapturer(THREAD_FILTERED_REDIRECT_ROUTER).capture(callable, CountDownLatch::new);
    }

    @Override
    public OngoingCapturedOutput copyOfThread(final ThrowingCallable callable) {
        return new AsynchronousOutputCapturer(THREAD_FILTERED_COPY_ROUTER).capture(callable, CountDownLatch::new);
    }

    @Override
    public CapturedOutput whileDoing(final ThrowingCallable callable) {
        return new SynchronousOutputCapturer(COPY_ROUTER).capture(callable);
    }

    @Override
    public OngoingCapturedOutput copyWhileDoing(final ThrowingCallable callable) {
        return new AsynchronousOutputCapturer(REDIRECT_ROUTER).capture(callable, CountDownLatch::new);
    }
}
