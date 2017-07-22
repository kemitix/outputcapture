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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Captures output written to {@code System::out} and {@code System::err} as a {@link CapturedOutput}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public final class CaptureOutput implements OutputCapturer {

    @Override
    public CapturedOutput of(final Runnable runnable) {
        return capture(runnable, selectCaptureStream());
    }

    @Override
    public CapturedOutput echoOf(final Runnable runnable) {
        return capture(runnable, TeeOutputStream::new);
    }

    private BiFunction<PrintStream, PrintStream, PrintStream> selectCaptureStream() {
        return (captureStream, originalStream) -> captureStream;
    }

    private CapturedOutput capture(
            final Runnable runnable, final BiFunction<PrintStream, PrintStream, PrintStream> streamWrapper
                                  ) {
        final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        final ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        final PrintStream originalOut = capturePrintStream(System.out, capturedOut, streamWrapper, System::setOut);
        final PrintStream originalErr = capturePrintStream(System.err, capturedErr, streamWrapper, System::setErr);
        runnable.run();
        System.setOut(originalOut);
        System.setErr(originalErr);
        return asCapturedOutput(capturedOut, capturedErr);
    }

    private CapturedOutput asCapturedOutput(
            final ByteArrayOutputStream capturedOut, final ByteArrayOutputStream capturedErr
                                           ) {
        return new CapturedOutput() {

            @Override
            public Stream<String> getStdOut() {
                return asStream(capturedOut);
            }

            @Override
            public Stream<String> getStdErr() {
                return asStream(capturedErr);
            }
        };
    }

    private PrintStream capturePrintStream(
            final PrintStream original, final ByteArrayOutputStream captureTo,
            final BiFunction<PrintStream, PrintStream, PrintStream> streamWrapper, final Consumer<PrintStream> setStream
                                          ) {
        setStream.accept(streamWrapper.apply(new PrintStream(captureTo), original));
        return original;
    }

    private Stream<String> asStream(final ByteArrayOutputStream captured) {
        return Arrays.stream(captured.toString()
                                     .split(System.lineSeparator()));
    }

}
