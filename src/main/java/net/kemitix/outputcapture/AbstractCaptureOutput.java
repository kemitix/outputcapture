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

import lombok.AccessLevel;
import lombok.Getter;
import net.kemitix.wrapper.printstream.PrintStreamWrapper;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base for capturing output written to {@code System::out} and {@code System::err} as a {@link CapturedOutput}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
abstract class AbstractCaptureOutput {

    @Getter(AccessLevel.PROTECTED)
    private AtomicReference<Exception> thrownException = new AtomicReference<>();

    private static Deque<CapturedOutput> activeCaptures = new ArrayDeque<>();
    private static PrintStream savedOut;
    private static PrintStream savedErr;

    /**
     * Invokes the Callable and stores any thrown exception.
     *
     * @param callable The callable to invoke
     */
    @SuppressWarnings("illegalcatch")
    protected void invokeCallable(final ThrowingCallable callable) {
        try {
            callable.call();
        } catch (Exception e) {
            thrownException.set(e);
        }
    }

    /**
     * Wait for the CountDownLatch to count down to 0.
     *
     * <p>Any {@link InterruptedException} that is thrown will be wrapped in an {@link OutputCaptureException}.</p>
     *
     * @param countDownLatch The latch to wait on
     */
    protected void awaitLatch(final CountDownLatch countDownLatch) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new OutputCaptureException("Error awaiting latch", e);
        }
    }

    protected void enable(final CapturedOutput capturedOutput, final Router router) {
        if (activeCaptures.isEmpty()) {
            savedOut = System.out;
            savedErr = System.err;
            System.setOut(PrintStreamWrapper.filter(savedOut, (PrintStreamWrapper.ByteFilter) aByte -> {
                for (CapturedOutput co : activeCaptures) {
                    if (router.accepts(aByte)) {
                        co.out().write(aByte);
                        if (router.isBlocking()) {
                            break;
                        }
                    }
                }
                return true;
            }));
            System.setErr(PrintStreamWrapper.filter(savedErr, (PrintStreamWrapper.ByteFilter) aByte -> {
                for (CapturedOutput co : activeCaptures) {
                    if (router.accepts(aByte)) {
                        co.err().write(aByte);
                        if (router.isBlocking()) {
                            break;
                        }
                    }
                }
                return true;
            }));
        }
        activeCaptures.addFirst(capturedOutput);
    }

    protected void disable(final CapturedOutput capturedOutput) {
        activeCaptures.remove(capturedOutput);
        if (activeCaptures.isEmpty()) {
            System.setOut(savedOut);
            System.setErr(savedErr);
        }
    }
}
