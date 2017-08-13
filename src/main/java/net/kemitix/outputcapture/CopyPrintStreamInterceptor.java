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

import lombok.NonNull;

import java.io.PrintStream;

/**
 * Interceptor for {@link PrintStream} that copies all writes to the supplied PrintStream and on to the intercepted
 * PrintStream, or to another interceptor.
 *
 * <p>If the Predicate returns {@code false} for the String, then it will not be written.</p>
 *
 * <p>N.B. only {@link #print(String)} and {@link #println(String)} will be checked against the Predicate. All other
 * {@code #write(*)}, {@code #print(*)} or {@code #println(*)} calls will always be written.</p>
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class CopyPrintStreamInterceptor extends PassthroughPrintStreamInterceptor {

    private final PrintStream copyTo;

    /**
     * Constructor to intercept in existing PrintStream.
     *
     * @param original the PrintStream to intercept
     * @param copyTo   the PrintStream to copy writes to
     */
    public CopyPrintStreamInterceptor(final PrintStream original, @NonNull final PrintStream copyTo) {
        super(original);
        this.copyTo = copyTo;
    }

    /**
     * Constructor to intercept in existing PrintStreamInterceptor.
     *
     * @param interceptor the interceptor to intercept
     * @param copyTo      the PrintStream to copy writes to
     */
    public CopyPrintStreamInterceptor(
            final PrintStreamInterceptor interceptor, @NonNull final PrintStream copyTo
                                     ) {
        super(interceptor);
        this.copyTo = copyTo;
    }

    @Override
    public final void write(final int b) {
        super.write(b);
        copyTo.write(b);
    }

    @Override
    public final void write(final byte[] buf, final int off, final int len) {
        super.write(buf, off, len);
        copyTo.write(buf, off, len);
    }
}
