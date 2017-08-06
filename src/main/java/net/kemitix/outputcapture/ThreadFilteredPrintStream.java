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
 * Filters output to a PrintStream to output written from the filtering thread.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class ThreadFilteredPrintStream extends PrintStream {

    private final Thread filteredThread;

    /**
     * Constructor.
     *
     * @param out            The output stream
     * @param filteredThread The thread to filter on
     */
    ThreadFilteredPrintStream(final OutputStream out, final Thread filteredThread) {
        super(out);
        this.filteredThread = filteredThread;
    }

    /**
     * Writes the specified byte to the output stream if the current thread is the filtered thread.
     *
     * @param b The byte to be written
     */
    @Override
    public void write(final int b) {
        if (onFilteredThread()) {
            super.write(b);
        }
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array starting at offset <code>off</code> to the output
     * stream if the current thread is the filtered thread.
     *
     * @param buf A byte array
     * @param off Offset from which to start taking bytes
     * @param len Number of bytes to write
     */
    @Override
    public void write(final byte[] buf, final int off, final int len) {
        if (onFilteredThread()) {
            super.write(buf, off, len);
        }
    }

    /** Writes the specified byte to the output stream if the parent thread is the filtered thread.
     *
     * @param b            The byte to be written
     * @param parentThread The parent thread
     */
    public void write(final int b, final Thread parentThread) {
        if (filteredThread.equals(parentThread)) {
            super.write(b);
        }
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array starting at offset <code>off</code> to the output
     * stream if the parent thread is the filtered thread.
     *
     * @param buf          A byte array
     * @param off          Offset from which to start taking bytes
     * @param len          Number of bytes to write
     * @param parentThread The parent thread
     */
    public void write(final byte[] buf, final int off, final int len, final Thread parentThread) {
        if (filteredThread.equals(parentThread)) {
            super.write(buf, off, len);
        }
    }

    private boolean onFilteredThread() {
        return filteredThread.equals(Thread.currentThread());
    }
}
