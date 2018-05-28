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

/**
 * Routes output between the capturing stream and the original stream.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
interface Router extends WritableChannels {

    /**
     * Returns true if the router is intercepting the stream, rather than passing it on the the next capture or output.
     *
     * @return true if next PrintStream should not receive any output
     */
    boolean isBlocking();

    /**
     * Returns true is the router will accept the byte.
     *
     * @param aByte the byte to be written.
     *
     * @return true if the byte should be written to the stream
     */
    boolean accepts(Byte aByte);

    /**
     * Fetch all the captured lines.
     *
     * @return the Captured Lines
     */
    CapturedLines getCapturedLines();

}
