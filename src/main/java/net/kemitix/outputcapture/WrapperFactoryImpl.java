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
import net.kemitix.wrapper.printstream.CopyPrintStreamWrapper;
import net.kemitix.wrapper.printstream.RedirectPrintStreamWrapper;

import java.io.PrintStream;

/**
 * Default implementation of the {@link WrapperFactory}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class WrapperFactoryImpl implements WrapperFactory {

    @Override
    public Wrapper<PrintStream> copyPrintStream(
            final PrintStream original,
            final PrintStream copyTo
                                               ) {
        return new CopyPrintStreamWrapper(original, copyTo);
    }

    @Override
    public Wrapper<PrintStream> copyPrintStream(
            final Wrapper<PrintStream> wrapped,
            final PrintStream copyTo
                                               ) {
        return new CopyPrintStreamWrapper(wrapped, copyTo);
    }

    @Override
    public Wrapper<PrintStream> redirectPrintStream(
            final PrintStream original,
            final PrintStream redirectTo
                                                   ) {
        return new RedirectPrintStreamWrapper(original, redirectTo);
    }

    @Override
    public Wrapper<PrintStream> redirectPrintStream(
            final Wrapper<PrintStream> wrapped,
            final PrintStream redirectTo
                                                   ) {
        return new RedirectPrintStreamWrapper(wrapped, redirectTo);
    }

    @Override
    public Wrapper<PrintStream> threadFilteredPrintStream(
            final Wrapper<PrintStream> wrapped,
            final Thread targetThread
                                                         ) {
        return new ThreadFilteredPrintStreamWrapper(wrapped, targetThread);
    }
}
