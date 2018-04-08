/*
  The MIT License (MIT)

  Copyright (c) 2018 Paul Campbell

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the "Software"), to deal in the Software without restriction,
  including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
  and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies
  or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
  AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.kemitix.outputcapture;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Base for holding captured output.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
abstract class AbstractCapturedOutput {

    /**
     * Converts the output stream into a stream of strings split by the system line separator.
     *
     * @param outputStream The output stream
     *
     * @return a Stream of Strings
     */
    Stream<String> asStream(final OutputStream outputStream) {
        final String string = outputStream.toString();
        if (string.length() == 0) {
            return Stream.empty();
        }
        return Arrays.stream(string.split(System.lineSeparator()));
    }

}
