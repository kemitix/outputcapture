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

import java.io.PrintStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Basic interceptor for {@link PrintStream} that simply passes all writes to the intercepted PrintStream, or to another
 * interceptor.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class PassthroughPrintStreamInterceptor extends PrintStream implements PrintStreamInterceptor {

    @Getter
    private final PrintStream printStream;

    private final AtomicReference<PrintStreamInterceptor> wrappedInterceptor = new AtomicReference<>();

    /**
     * Constructor to intercept a PrintStream.
     *
     * @param original the PrintStream to intercept
     */
    public PassthroughPrintStreamInterceptor(final PrintStream original) {
        super(original);
        this.printStream = original;
    }

    /**
     * Constructor to intercept in existing PrintStreamInterceptor.
     *
     * @param interceptor the interceptor to intercept
     */
    public PassthroughPrintStreamInterceptor(final PrintStreamInterceptor interceptor) {
        super(Objects.requireNonNull(interceptor, "interceptor").getPrintStream());
        this.printStream = interceptor.getPrintStream();
        this.wrappedInterceptor.set(interceptor);
    }

    /**
     * Writes the specified byte to this stream.
     *
     * <p>If the byte is a newline and automatic flushing is enabled then the flush method will be invoked.</p>
     *
     * <p>Note that the byte is written as given; to write a character that will be translated according to the
     * platform's default character encoding, use the print(char) or println(char) methods.</p>
     *
     * <p>This implementation passes the byte, unmodified, to the intercepted {@link PrintStream} or {@link
     * PrintStreamInterceptor}.</p>
     *
     * @param b The byte to be written
     *
     * @see #print(char)
     * @see #println(char)
     */
    @Override
    public void write(final int b) {
        if (wrappedInterceptor.get() != null) {
            wrappedInterceptor.get()
                              .asPrintStream()
                              .write(b);
        } else {
            printStream.write(b);
        }
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off to this stream.
     *
     * <p>If automatic flushing is enabled then the flush method will be invoked.</p>
     *
     * <p>Note that the bytes will be written as given; to write characters that will be translated according to the
     * platform's default character encoding, use the print(char) or println(char) methods.</p>
     *
     * <p>This implementation passes the bytes, unmodified, to the intercepted {@link PrintStream} or {@link
     * PrintStreamInterceptor}.</p>
     *
     * @param buf A byte array
     * @param off Offset from which to start taking bytes
     * @param len Number of bytes to write
     */
    @Override
    public void write(@NonNull final byte[] buf, final int off, final int len) {
        if (wrappedInterceptor.get() != null) {
            wrappedInterceptor.get()
                              .asPrintStream()
                              .write(buf, off, len);
        } else {
            printStream.write(buf, off, len);
        }
    }

    @Override
    public final Optional<PrintStreamInterceptor> getWrappedInterceptor() {
        return Optional.ofNullable(wrappedInterceptor.get());
    }

    @Override
    public final void remove(@NonNull final PrintStreamInterceptor interceptor) {
        if (wrappedInterceptor.compareAndSet(interceptor, null)) {
            return;
        }
        Optional.ofNullable(wrappedInterceptor.get())
                .ifPresent(wrapped -> wrapped.remove(interceptor));
    }

    @Override
    public final PrintStream asPrintStream() {
        return this;
    }
}
