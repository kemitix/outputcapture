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

import net.kemitix.wrapper.Wrapper;

import java.io.PrintStream;

/**
 * Factory for creating {@link Wrapper}s.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
interface WrapperFactory {

    /**
     * Create a {@link Wrapper} for the original PrintStream that copies all output written to the other PrintStream.
     *
     * @param original The original PrintStream
     * @param copyTo   The PrintStream to copy all writes to
     *
     * @return an instance of a {@code Wrapper<PrintStream>}
     */
    Wrapper<PrintStream> copyPrintStream(
            PrintStream original,
            PrintStream copyTo
                                        );

    /**
     * Create a {@link Wrapper} for the original PrintStream that redirects all output written to the other PrintStream.
     *
     * @param original   The original PrintStream
     * @param redirectTo The PrintStream to redirect all writes to
     *
     * @return an instance of a {@code Wrapper<PrintStream>}
     */
    Wrapper<PrintStream> redirectPrintStream(
            PrintStream original,
            PrintStream redirectTo
                            );
}
