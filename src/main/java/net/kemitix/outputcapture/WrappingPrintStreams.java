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

import lombok.Getter;
import lombok.NonNull;
import net.kemitix.wrapper.Wrapper;

import java.io.PrintStream;

/**
 * A simple collection of PrintStream Wrappers, with one main wrapper and any number of others.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class WrappingPrintStreams {

    @Getter
    private final Wrapper<PrintStream> mainWrapper;

    @Getter
    private final Wrapper<PrintStream>[] otherWrappers;

    /**
     * Constructor.
     *
     * @param mainWrapper   The main Wrapper
     * @param otherWrappers The other Wrappers
     */
    WrappingPrintStreams(
            @NonNull final Wrapper<PrintStream> mainWrapper,
            final Wrapper<PrintStream>... otherWrappers
                        ) {
        this.mainWrapper = mainWrapper;
        this.otherWrappers = otherWrappers;
    }
}
