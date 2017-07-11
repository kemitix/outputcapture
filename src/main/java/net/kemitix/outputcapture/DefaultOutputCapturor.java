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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Default implementation of {@link OutputCapture}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class DefaultOutputCapturor implements OutputCapture {

    private final ByteArrayOutputStream out;

    private final PrintStream savedOut;

    private final ByteArrayOutputStream err;

    private final PrintStream savedErr;

    private final PrintStream outWrapper;

    /**
     * Begin capturing output.
     */
    DefaultOutputCapturor() {
        savedOut = System.out;
        out = new ByteArrayOutputStream();
        outWrapper = new PrintStream(out);
        System.setOut(outWrapper);
        savedErr = System.err;
        err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
    }

    @Override
    public Stream<String> getStdOut() {
        return Arrays.stream(out.toString()
                                .split(System.lineSeparator()));
    }

    @Override
    public Stream<String> getStdErr() {
        return Arrays.stream(err.toString()
                                .split(System.lineSeparator()));
    }

    @Override
    public void clear() {
        out.reset();
        err.reset();
    }

    @Override
    public void close() throws IOException {
        if (!System.out.equals(outWrapper)) {
            throw new IOException("Not the current capture. Close the current capture first");
        }
        System.setOut(savedOut);
        System.setErr(savedErr);
    }

}
