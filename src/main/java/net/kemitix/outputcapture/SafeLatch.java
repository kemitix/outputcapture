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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch that, when an {@link InterruptedException} is thrown, wraps it in a {@link OutputCaptureException}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class SafeLatch extends CountDownLatch {

    private final Long maxAwaitMilliseconds;
    private final Runnable interruptHandler;

    @Getter
    private boolean timedout = false;

    /**
     * Constructs a {@code CountDownLatch} initialized with the given count.
     *
     * @param count                the number of times {@link #countDown} must be invoke before threads can pass
     *                             through {@link #await}
     * @param maxAwaitMilliseconds the maximum number of milliseconds to await for the latch to complete
     * @param interruptHandler     the Runnable to execute if the await() methods are interrupted
     */
    public SafeLatch(final int count, final Long maxAwaitMilliseconds, final Runnable interruptHandler) {
        super(count);
        this.maxAwaitMilliseconds = maxAwaitMilliseconds;
        this.interruptHandler = interruptHandler;
    }

    /**
     * Blocks the thread and waits for the latch to finish counting down before returning.
     *
     * <p>This implementation wraps any InterruptedException thrown in an OutputCaptureException.</p>
     */
    @Override
    public void await() {
        try {
            timedout = !super.await(maxAwaitMilliseconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            interruptHandler.run();
        }
    }

}
