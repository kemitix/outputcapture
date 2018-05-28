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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a single line fo captured output.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public interface CapturedOutputLine {

    /**
     * Create a CapturedOutputLine, written to standard out, for the list of bytes.
     *
     * @param bytes the bytes that were written
     * @return a CapturedOutputLine where isOut() is true
     */
    static CapturedOutputLine out(final List<Byte> bytes) {
        final String string = byteListToString(bytes);
        return new CapturedOutputLine() {
            @Override
            public boolean isOut() {
                return true;
            }

            @Override
            public boolean isErr() {
                return false;
            }

            @Override
            public String asString() {
                return string;
            }
        };
    }

    /**
     * Create a CapturedOutputLine, written to standard error, for the list of bytes.
     *
     * @param bytes the bytes that were written
     * @return a CapturedOutputLine where isErr() is true
     */
    static CapturedOutputLine err(final List<Byte> bytes) {
        final String string = byteListToString(bytes);
        return new CapturedOutputLine() {
            @Override
            public boolean isOut() {
                return false;
            }

            @Override
            public boolean isErr() {
                return true;
            }

            @Override
            public String asString() {
                return string;
            }
        };
    }

    /**
     * Helper for converting a list of bytes into a String.
     *
     * @param bytes the bytes
     * @return a String
     */
    static String byteListToString(final List<Byte> bytes) {
        final byte[] byteArray = new byte[bytes.size()];
        final AtomicInteger idx = new AtomicInteger(0);
        bytes.forEach(b -> byteArray[idx.getAndIncrement()] = b);
        return new String(byteArray, StandardCharsets.UTF_8);
    }

    /**
     * Returns true the line was written to standard out.
     *
     * @return true if written to standard out
     */
    boolean isOut();

    /**
     * Returns true the line was written to standard error.
     *
     * @return true if written to standard err
     */
    boolean isErr();

    /**
     * Returns the line as a string.
     *
     * @return the line that was written
     */
    String asString();

}
