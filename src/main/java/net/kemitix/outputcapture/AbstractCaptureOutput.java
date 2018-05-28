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

import lombok.AccessLevel;
import lombok.Getter;
import net.kemitix.wrapper.printstream.PrintStreamWrapper;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base for capturing output written to {@code System::out} and {@code System::err} as a {@link CapturedOutput}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
abstract class AbstractCaptureOutput implements CaptureOutput {

    private static final Deque<RoutableCapturedOutput> ACTIVE_CAPTURES = new ArrayDeque<>();
    private static PrintStream savedOut;
    private static PrintStream savedErr;

    @Getter(AccessLevel.PROTECTED)
    private AtomicReference<Exception> thrownExceptionReference = new AtomicReference<>();

    /**
     * Invokes the Callable and stores any thrown exception.
     *
     * @param callable The callable to invoke
     */
    @SuppressWarnings("illegalcatch")
    void invokeCallable(final ThrowingCallable callable) {
        try {
            callable.call();
        } catch (Exception e) {
            thrownExceptionReference.set(e);
        }
    }

    /**
     * Begin passing output to the {@link CaptureOutput}, before any other captures that may already be in place.
     *
     * @param capturedOutput the recipient of any future output
     */
    void enable(final RoutableCapturedOutput capturedOutput) {
        synchronized (ACTIVE_CAPTURES) {
            if (ACTIVE_CAPTURES.isEmpty()) {
                savedOut = System.out;
                savedErr = System.err;
                System.setOut(PrintStreamWrapper.filter(savedOut, captureSystemOutFilter()));
                System.setErr(PrintStreamWrapper.filter(savedErr, captureSystemErrFilter()));
            }
            ACTIVE_CAPTURES.addFirst(capturedOutput);
        }
    }

    /**
     * Stop passing output to the {@link CaptureOutput}.
     *
     * @param capturedOutput the recipient to remove
     */
    void disable(final RoutableCapturedOutput capturedOutput) {
        synchronized (ACTIVE_CAPTURES) {
            ACTIVE_CAPTURES.remove(capturedOutput);
            if (ACTIVE_CAPTURES.isEmpty()) {
                System.setOut(savedOut);
                System.setErr(savedErr);
            }
        }
    }

    private static PrintStreamWrapper.ByteFilter captureSystemErrFilter() {
        return aByte -> {
            for (RoutableCapturedOutput co : ACTIVE_CAPTURES) {
                final Router router = co.getRouter();
                if (router.accepts(aByte)) {
                    router.writeErr(aByte);
                    co.err().write(aByte);
                    if (router.isBlocking()) {
                        break;
                    }
                }
            }
            return true;
        };
    }

    private static PrintStreamWrapper.ByteFilter captureSystemOutFilter() {
        return aByte -> {
            for (RoutableCapturedOutput co : ACTIVE_CAPTURES) {
                final Router router = co.getRouter();
                if (router.accepts(aByte)) {
                    router.writeOut(aByte);
                    co.out().write(aByte);
                    if (router.isBlocking()) {
                        break;
                    }
                }
            }
            return true;
        };
    }

    /**
     * The number of active captures in place.
     *
     * @return a count the {@code OutputCapturer} instances in effect
     */
    static int activeCount() {
        return ACTIVE_CAPTURES.size();
    }

    /**
     * Remove any active captures.
     */
    static void removeAllActiveCaptures() {
        while (!ACTIVE_CAPTURES.isEmpty()) {
            ACTIVE_CAPTURES.poll();
        }
        System.setOut(savedOut);
        System.setErr(savedErr);
    }
}
