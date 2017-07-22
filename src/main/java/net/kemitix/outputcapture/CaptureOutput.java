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
        return capture(runnable, new RedirectRouter());
    }

    @Override
    public CapturedOutput echoOf(final Runnable runnable) {
        return capture(runnable, new CopyRouter());
    }

    private CapturedOutput capture(
            final Runnable runnable, final Router router
                                  ) {
        final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        final ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        final PrintStream originalOut = capturePrintStream(System.out, capturedOut, router, System::setOut);
        final PrintStream originalErr = capturePrintStream(System.err, capturedErr, router, System::setErr);
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
            final PrintStream originalStream, final ByteArrayOutputStream captureTo, final Router router,
            final Consumer<PrintStream> setStream
                                          ) {
        final PrintStream capturingStream = new PrintStream(captureTo);
        final PrintStream routedStream = router.handle(capturingStream, originalStream);
        setStream.accept(routedStream);
        return originalStream;
    }

    private Stream<String> asStream(final ByteArrayOutputStream captured) {
        return Arrays.stream(captured.toString()
                                     .split(System.lineSeparator()));
    }

    /**
     * Routes output between the capturing stream and the original stream.
     *
     * @see {@link RedirectRouter}
     * @see {@link CopyRouter}
     */
    private interface Router {

        /**
         * Create an output stream that routes the output to the appropriate stream(s).
         *
         * @param capturingStream the stream capturing the output
         * @param originalStream  the stream where output would normally have gone
         *
         * @return a PrintStream to be used to write to
         */
        PrintStream handle(PrintStream capturingStream, PrintStream originalStream);

    }

    /**
     * Router that redirects output away from the original output stream to the capturing stream.
     */
    private class RedirectRouter implements Router {

        @Override
        public PrintStream handle(final PrintStream capturingStream, final PrintStream originalStream) {
            return capturingStream;
        }
    }

    /**
     * Router the copies the output to both the original output stream and the capturing stream.
     */
    private class CopyRouter implements Router {

        @Override
        public PrintStream handle(final PrintStream capturingStream, final PrintStream originalStream) {
            return new TeeOutputStream(capturingStream, originalStream);
        }
    }
}
