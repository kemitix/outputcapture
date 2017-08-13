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
import java.util.function.Predicate;

/**
 * Interceptor for {@link PrintStream} that tests Strings with a supplied {@link Predicate} before writing to the
 * intercepted PrintStream, or to another interceptor.
 *
 * <p>If the Predicate returns {@code false} for the String, then it will not be written.</p>
 *
 * <p>N.B. only {@link #print(String)} and {@link #println(String)} will be checked against the Predicate. All other
 * {@code #write(*)}, {@code #print(*)} or {@code #println(*)} calls will always be written.</p>
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class FilteredPrintStreamInterceptor extends PassthroughPrintStreamInterceptor {

    private final Predicate<String> predicate;

    /**
     * Constructor to intercept in existing PrintStream.
     *
     * @param out       the PrintStream to intercept
     * @param predicate the predicate to apply to strings
     */
    public FilteredPrintStreamInterceptor(
            final PrintStream out, final Predicate<String> predicate
                                         ) {
        super(out);
        this.predicate = predicate;
    }

    /**
     * Constructor to intercept in existing PrintStreamInterceptor.
     *
     * @param interceptor the interceptor to intercept
     * @param predicate   the predicate to apply to strings
     */
    public FilteredPrintStreamInterceptor(
            final PrintStreamInterceptor interceptor, final Predicate<String> predicate
                                         ) {
        super(interceptor);
        this.predicate = predicate;
    }

    @Override
    public final void print(final String s) {
        if (predicate.test(s)) {
            super.print(s);
        }
    }

    @Override
    public final void println(final String s) {
        if (predicate.test(s)) {
            super.println(s);
        }
    }
}
