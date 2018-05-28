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

import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Implementation of CapturedLines.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
@RequiredArgsConstructor
class DefaultCapturedLines implements CapturedLines {

    private final List<CapturedOutputLine> lines = new LinkedList<>();
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final AtomicReference<Byte> lastOut = new AtomicReference<>((byte) 0);
    private final AtomicReference<Byte> lastErr = new AtomicReference<>((byte) 0);
    private final List<Byte> currentLineOut = new ArrayList<>();
    private final List<Byte> currentLineErr = new ArrayList<>();
    private final String lineSeparator;

    @Override
    public Stream<CapturedOutputLine> stream() {
        return lines.stream();
    }

    @Override
    public void writeOut(final Byte aByte) {
        if (isNewLine(lastOut.get(), aByte)) {
            if (lineSeparator.length() > 1) {
                currentLineOut.remove(currentLineOut.size() - 1);
            }
            lines.add(CapturedOutputLine.out(currentLineOut));
            currentLineOut.clear();
            out.reset();
        } else {
            currentLineOut.add(aByte);
            out.write(aByte);
        }
        lastOut.set(aByte);
    }

    @Override
    public void writeErr(final Byte aByte) {
        if (isNewLine(lastErr.get(), aByte)) {
            if (lineSeparator.length() > 1) {
                currentLineErr.remove(currentLineErr.size() - 1);
            }
            lines.add(CapturedOutputLine.err(currentLineErr));
            currentLineErr.clear();
            err.reset();
        } else {
            currentLineErr.add(aByte);
            err.write(aByte);
        }
        lastErr.set(aByte);
    }

    private boolean isNewLine(final Byte previous, final Byte current) {
        if (lineSeparator.length() > 1) {
            return lineSeparator.equals(new String(new byte[] {previous, current}));
        } else {
            return lineSeparator.equals(new String(new byte[] {current}));
        }
    }
}
