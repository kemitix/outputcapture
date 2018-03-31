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

import java.io.ByteArrayOutputStream;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The captured output written to System.out and System.err.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class DefaultCapturedOutput extends AbstractCapturedOutput implements CapturedOutput {

    @Getter(AccessLevel.PROTECTED)
    private final ByteArrayOutputStream capturedOut;

    @Getter(AccessLevel.PROTECTED)
    private final ByteArrayOutputStream capturedErr;

    /**
     * Constructor.
     *
     * @param capturedOut The captured output written to System.out
     * @param capturedErr The captured output written to System.err
     */
    DefaultCapturedOutput(final ByteArrayOutputStream capturedOut, final ByteArrayOutputStream capturedErr) {
        this.capturedOut = capturedOut;
        this.capturedErr = capturedErr;
    }

    @Override
    public Stream<String> getStdOut() {
        return asStream(capturedOut);
    }

    @Override
    public Stream<String> getStdErr() {
        return asStream(capturedErr);
    }

    @Override
    public boolean test(Byte aByte) {
        return true;
    }

    @Override
    public Function<Byte, Boolean> out() {
        return b -> {
            if (test(b)) {
                capturedOut.write(b);
            }
            return true;
        };
    }

    @Override
    public Function<Byte, Boolean> err() {
        return b -> {
            if (test(b)) {
                capturedErr.write(b);
            }
            return true;
        };
    }
}
