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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

/**
 * Copies output to all streams.
 *
 * <p>Where one of the {@code extraOutputStreams} is an instance of {@link ThreadFilteredPrintStream}, then the
 * {@code parentThread} is provided to allow it to perform its filtering.</p>
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 *
 * @see ThreadFilteredPrintStream
 */
class TeeOutputStream extends PrintStream {

    private final Thread parentThread;

    private final List<PrintStream> outputStreams;

    /**
     * Constructor.
     *
     * @param outputStream       the first output stream, mandatory
     * @param parentThread       The thread that invoked the output capture and the owner of the extra streams
     * @param extraOutputStreams the extra output streams
     */
    TeeOutputStream(
            final PrintStream outputStream, final Thread parentThread, final PrintStream... extraOutputStreams
                   ) {
        super(outputStream);
        this.parentThread = parentThread;
        this.outputStreams = Arrays.asList(extraOutputStreams);
    }

    /**
     * Writes the specified byte to all streams.
     *
     * @param b The byte to be written
     */
    @Override
    public void write(final int b) {
        super.write(b);
        outputStreams.forEach(os -> {
            if (os instanceof ThreadFilteredPrintStream) {
                ((ThreadFilteredPrintStream) os).write(b, parentThread);
            } else {
                os.write(b);
            }
        });
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array starting at offset <code>off</code> to all streams.
     *
     * @param buf A byte array
     * @param off Offset from which to start taking bytes
     * @param len Number of bytes to write
     */
    @Override
    public void write(final byte[] buf, final int off, final int len) {
        super.write(buf, off, len);
        outputStreams.forEach(os -> {
            if (os instanceof ThreadFilteredPrintStream) {
                ((ThreadFilteredPrintStream) os).write(buf, off, len, parentThread);
            } else {
                os.write(buf, off, len);
            }
        });
    }
}
