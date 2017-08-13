/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Paul Campbell
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.kemitix.outputcapture;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.BDDMockito.then;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests for {@link PassthroughPrintStreamInterceptor}s.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class PassthroughPrintStreamInterceptorTest {

    @Mock
    private PrintStream intercepted;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void canWriteAByteUnmodified() {
        //given
        final PrintStream interceptor = new TestPrintStreamInterceptor(intercepted);
        //when
        final ThrowableAssert.ThrowingCallable code = () -> interceptor.write('x');
        //then
        assertThatCode(code).doesNotThrowAnyException();
        then(intercepted).should()
                         .write('x');
    }

    @Test
    public void canWriteByteArrayUnmodified() throws IOException {
        //given
        final PrintStream interceptor = new TestPrintStreamInterceptor(intercepted);
        //when
        final ThrowableAssert.ThrowingCallable code = () -> interceptor.write("test".getBytes());
        //then
        assertThatCode(code).doesNotThrowAnyException();
        then(intercepted).should()
                         .write("test".getBytes(), 0, 4);
    }

    @Test
    public void writeNullByteArrayWillThrowNullPointerException() throws IOException {
        //given
        final PrintStream interceptor = new TestPrintStreamInterceptor(intercepted);
        //when
        final ThrowableAssert.ThrowingCallable code = () -> interceptor.write(null);
        //then
        assertThatNullPointerException().isThrownBy(code);
    }

    @Test
    public void canGetOriginalPrintStreamFromSingleInterceptor() {
        //given
        final PrintStreamInterceptor interceptor = new TestPrintStreamInterceptor(intercepted);
        //when
        final PrintStream result = interceptor.getPrintStream();
        //then
        assertThat(result).isSameAs(intercepted);
    }

    @Test
    public void canGetOriginalPrintStreamThroughMultipleInterceptors() {
        //given
        final PrintStreamInterceptor existing = new TestPrintStreamInterceptor(intercepted);
        final PrintStreamInterceptor interceptor = new TestPrintStreamInterceptor(existing);
        //when
        final PrintStream result = interceptor.getPrintStream();
        //then
        assertThat(result).isSameAs(intercepted);
    }

    @Test
    public void writeByteToSecondInterceptorDelegatesThroughFirstToOriginal() {
        //given
        final TestPrintStreamInterceptor existing = new TestPrintStreamInterceptor(intercepted);
        final TestPrintStreamInterceptor interceptor =
                new TestPrintStreamInterceptor((PrintStreamInterceptor) existing);
        //when
        interceptor.asPrintStream()
                   .write('x');
        //then
        assertThat(interceptor.getWritten()).isEqualTo("x");
        assertThat(existing.getWritten()).isEqualTo("x");
        then(intercepted).should()
                         .write('x');
    }

    @Test
    public void writeByteArrayToSecondInterceptorDelegatesThroughFirstToOriginal() throws IOException {
        //given
        final TestPrintStreamInterceptor existing = new TestPrintStreamInterceptor(intercepted);
        final TestPrintStreamInterceptor interceptor =
                new TestPrintStreamInterceptor((PrintStreamInterceptor) existing);
        //when
        interceptor.asPrintStream()
                   .write("test".getBytes());
        //then
        assertThat(interceptor.getWritten()).isEqualTo("test");
        assertThat(existing.getWritten()).isEqualTo("test");
        then(intercepted).should()
                         .write("test".getBytes(), 0, 4);
    }

    @Test
    public void canGetOriginalInterceptor() {
        //given
        final PrintStreamInterceptor first = new TestPrintStreamInterceptor(intercepted);
        final PrintStreamInterceptor second = new TestPrintStreamInterceptor(first);
        //when
        final Optional<PrintStreamInterceptor> result = second.getWrappedInterceptor();
        //then
        assertThat(result).contains(first);
    }

    @Test
    public void whenTwoInterceptorsAndFirstIsRemovedThenCanWriteByte() throws IOException {
        //given
        final TestPrintStreamInterceptor first = new TestPrintStreamInterceptor(intercepted);
        final TestPrintStreamInterceptor second = new TestPrintStreamInterceptor((PrintStreamInterceptor) first);
        //when
        second.remove(first);
        second.asPrintStream()
              .write('x');
        //then
        assertThat(second.getWrappedInterceptor()).isEmpty();
        assertThat(first.getWritten()).isEmpty();
        then(intercepted).should()
                         .write('x');
    }

    @Test
    public void whenTwoInterceptorsAndFirstIsRemovedThenCanWriteByteArray() throws IOException {
        //given
        final TestPrintStreamInterceptor first = new TestPrintStreamInterceptor(intercepted);
        final TestPrintStreamInterceptor second = new TestPrintStreamInterceptor((PrintStreamInterceptor) first);
        //when
        second.remove(first);
        second.asPrintStream()
              .write("test".getBytes());
        //then
        assertThat(second.getWrappedInterceptor()).isEmpty();
        assertThat(first.getWritten()).isEmpty();
        then(intercepted).should()
                         .write("test".getBytes(), 0, 4);
    }

    @Test
    public void whenTwoInterceptorsAndFirstIsRemovedThenCanWriteByteArraySubset() throws IOException {
        //given
        final TestPrintStreamInterceptor first = new TestPrintStreamInterceptor(intercepted);
        final TestPrintStreamInterceptor second = new TestPrintStreamInterceptor((PrintStreamInterceptor) first);
        //when
        second.remove(first);
        second.asPrintStream()
              .write("test".getBytes(), 1, 2);
        //then
        assertThat(second.getWrappedInterceptor()).isEmpty();
        assertThat(first.getWritten()).isEmpty();
        then(intercepted).should()
                         .write("test".getBytes(), 1, 2);
    }

    @Test
    public void whenRemoveInterceptorIsNullThenThrowNullException() {
        //given
        final PrintStreamInterceptor first = new TestPrintStreamInterceptor(intercepted);
        final PrintStreamInterceptor second = new TestPrintStreamInterceptor(first);
        //when
        assertThatNullPointerException().isThrownBy(() -> second.remove(null))
                                        .withMessage("interceptor");
    }

    @Test public void whenThreeInterceptorsAndFirstIsRemovedThenOthersRemain() {
        //given
        final TestPrintStreamInterceptor first = new TestPrintStreamInterceptor(intercepted);
        final TestPrintStreamInterceptor second = new TestPrintStreamInterceptor(((PrintStreamInterceptor) first));
        final TestPrintStreamInterceptor third = new TestPrintStreamInterceptor(((PrintStreamInterceptor) second));
        //when
        third.remove(first);
        //then
        assertThat(third.getWrappedInterceptor()).contains(second);
        assertThat(second.getWrappedInterceptor()).isEmpty();
    }

    private class TestPrintStreamInterceptor extends PassthroughPrintStreamInterceptor {

        private StringBuilder bytesWritten = new StringBuilder();

        TestPrintStreamInterceptor(final PrintStream out) {
            super(out);
        }

        TestPrintStreamInterceptor(final PrintStreamInterceptor interceptor) {
            super(interceptor);
        }

        @Override
        public void write(final int b) {
            super.write(b);
            bytesWritten.append((char) b);
        }

        @Override
        public void write(final byte[] buf, final int off, final int len) {
            super.write(buf, off, len);
            bytesWritten.append(new String(buf).toCharArray(), off, len);
        }

        String getWritten() {
            return bytesWritten.toString();
        }
    }
}
