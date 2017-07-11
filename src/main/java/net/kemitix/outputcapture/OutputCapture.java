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

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Captures the {@code System.out} and {@code System.err}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public interface OutputCapture extends AutoCloseable {

    /**
     * Get a stream of the captured standard output so far.
     *
     * @return a Stream of Strings, one line per String using the system's line separator
     */
    Stream<String> getStdOut();

    /**
     * Create a new OutputCapture and begin capturing the output.
     *
     * @return an OutputCapture instance
     */
    static OutputCapture begin() {
        return new DefaultOutputCapturor();
    }

    /**
     * Clears all output already captured.
     */
    void clear();

    /**
     * Get a stream of the captured standard error so far.
     *
     * @return a Stream of Strings, one line per String using the system's line separator
     */
    Stream<String> getStdErr();

    /**
     * Closes capture and restores the original output destinations for standard output and error.
     *
     * <p>If the capture is already closed then invoking this method has no effect.</p>
     *
     * <p>As noted in {@link AutoCloseable#close()}, cases where the close may fail require careful attention. It is
     * strongly advised to relinquish the underlying resources and to internally <em>mark</em> the {@code Closeable} as
     * closed, prior to throwing the {@code IOException}.</p>
     *
     * @throws IOException if there was an error restoring the original output streams
     */
    void close() throws IOException;

}
