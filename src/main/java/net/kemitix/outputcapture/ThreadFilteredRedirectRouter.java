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

import lombok.val;
import net.kemitix.wrapper.Wrapper;

import java.io.PrintStream;

/**
 * Router that redirects output from the target thread away from the original output stream to the capturing stream.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class ThreadFilteredRedirectRouter extends RedirectRouter {

    private final WrapperFactory wrapperFactory;

    /**
     * Constructor.
     *
     * @param wrapperFactory The Wrapper Factory
     */
    ThreadFilteredRedirectRouter(final WrapperFactory wrapperFactory) {
        super(wrapperFactory);
        this.wrapperFactory = wrapperFactory;
    }

    @Override
    public WrappingPrintStreams createWrappedPrintStream(
            final Wrapper<PrintStream> wrapped, final Thread targetThread
                                                        ) {
        val threadFilterWrapper = wrapperFactory.threadFilteredPrintStream(wrapped, targetThread);
        return new WrappingPrintStreams(threadFilterWrapper, wrapped);
    }
}