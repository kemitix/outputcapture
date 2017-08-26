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
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kemitix.wrapper.Wrapper;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Router the copies the output to both the original output stream and the capturing stream.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
@RequiredArgsConstructor
class CopyRouter implements Router {

    private final WrapperFactory wrapperFactory;

    @Override
    public WrappingPrintStreams wrap(
            @NonNull final OutputStream captureTo,
            @NonNull final PrintStream originalStream,
            final Thread targetThread
                                    ) {
        val copyTo = new PrintStream(captureTo);
        val wrapped = Wrapper.asWrapper(originalStream)
                             .map(wrapWrapper(copyTo))
                             .orElseGet(wrapPrintStream(originalStream, copyTo));
        return createWrappedPrintStream(wrapped, targetThread);
    }

    private Function<Wrapper<PrintStream>, Wrapper<PrintStream>> wrapWrapper(final PrintStream copyTo) {
        return wrapper -> wrapperFactory.copyPrintStream(wrapper, copyTo);
    }

    private Supplier<Wrapper<PrintStream>> wrapPrintStream(
            final PrintStream originalStream,
            final PrintStream copyTo
                                                            ) {
        return () -> wrapperFactory.copyPrintStream(originalStream, copyTo);
    }

}
