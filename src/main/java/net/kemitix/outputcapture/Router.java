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

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Routes output between the capturing stream and the original stream.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
interface Router {

    /**
     * Create an output stream that routes the output to the appropriate stream(s).
     *
     * @param captureTo      the output stream capturing the output
     * @param originalStream the stream where output would normally have gone
     * @param targetThread   the thread to filter on, if filtering is required
     *
     * @return a PrintStream to be used to write to
     */
    WrappingPrintStreams wrap(OutputStream captureTo, PrintStream originalStream, Thread targetThread);

    /**
     * Creates a WrappingPrintStreams object.
     *
     * <p>This default implementation creates an object containing only the wrapper provided. The targetThread is
     * ignored.</p>
     *
     * @param wrapped      The main PrintStream Wrapper
     * @param targetThread The target Thread for filtering, if used
     *
     * @return a WrappingPrintStreams instance containing {@code wrapped}
     */
    default WrappingPrintStreams createWrappedPrintStream(
            final PrintStream wrapped,
            final Thread targetThread
                                                         ) {
        return new WrappingPrintStreams(wrapped);
    }
}
