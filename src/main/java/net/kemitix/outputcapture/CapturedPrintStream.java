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

import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Holder for the original stream and its replacement.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class CapturedPrintStream {

    @Getter
    private final PrintStream originalStream;

    @Getter
    private final PrintStream replacementStream;

    @Getter
    private final ByteArrayOutputStream capturedTo;

    /**
     * Constructor.
     *
     * @param originalStream The original stream
     * @param router         The output router
     * @param parentThread   The thread that invoked the output capture and the owner of the original stream
     */
    CapturedPrintStream(final PrintStream originalStream, final Router router, final Thread parentThread) {
        this.originalStream = originalStream;
        this.capturedTo = new ByteArrayOutputStream();
        final PrintStream capturingStream = new PrintStream(this.capturedTo);
        final PrintStream filteredStream = new ThreadFilteredPrintStream(capturingStream, Thread.currentThread());
        this.replacementStream = router.handle(filteredStream, this.originalStream, parentThread);
    }
}
