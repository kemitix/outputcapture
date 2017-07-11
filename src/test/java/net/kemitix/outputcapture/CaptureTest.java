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

import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for capturing output.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class CaptureTest {

    private String line1;

    private String line2;

    @Before
    public void setUp() throws Exception {
        line1 = randomText();
        line2 = randomText();
    }

    @Test
    public void canCaptureSystemOut() {
        //given
        final OutputCapture capture = OutputCapture.begin();
        //when
        System.out.println(line1);
        System.out.println(line2);
        //then
        assertThat(capture.getStdOut()).containsExactly(line1, line2);
    }

    @Test
    public void canCaptureSystemErr() {
        //given
        final OutputCapture capture = OutputCapture.begin();
        //when
        System.err.println(line1);
        System.err.println(line2);
        //then
        assertThat(capture.getStdErr()).containsExactly(line1, line2);
    }

    @Test
    public void canRestoreNormalSystemOut() throws Exception {
        //given
        final OutputCapture originalCapture = OutputCapture.begin();
        final OutputCapture capture = OutputCapture.begin();
        System.out.println(line1);
        //when
        capture.close();
        System.out.println(line2);
        //then
        assertThat(capture.getStdOut()).containsExactly(line1)
                                       .doesNotContain(line2);
        assertThat(originalCapture.getStdOut()).containsExactly(line2)
                                               .doesNotContain(line1);
    }

    @Test
    public void canRestoreNormalSystemErr() throws Exception {
        //given
        final OutputCapture originalCapture = OutputCapture.begin();
        final OutputCapture capture = OutputCapture.begin();
        System.err.println(line1);
        //when
        capture.close();
        System.err.println(line2);
        //then
        assertThat(capture.getStdErr()).containsExactly(line1)
                                       .doesNotContain(line2);
        assertThat(originalCapture.getStdErr()).containsExactly(line2)
                                               .doesNotContain(line1);
    }

    @Test
    public void canClearCapturedOutput() {
        //given
        final OutputCapture capture = OutputCapture.begin();
        //when
        System.out.println(line1);
        capture.clear();
        System.out.println(line2);
        //then
        assertThat(capture.getStdOut()).containsExactly(line2)
                                       .doesNotContain(line1);
    }

    @Test
    public void canClearCapturedError() {
        //given
        final OutputCapture capture = OutputCapture.begin();
        //when
        System.err.println(line1);
        capture.clear();
        System.err.println(line2);
        //then
        assertThat(capture.getStdErr()).containsExactly(line2)
                                       .doesNotContain(line1);
    }

    @Test
    public void canUseTryWithResourcesSyntax() throws Exception {
        try (OutputCapture capture = OutputCapture.begin()) {
            System.out.println(line1);
            assertThat(capture.getStdOut()).containsExactly(line1);
        }
    }

    @Test
    public void throwsIOExceptionWhenCaptureIsNotActive() throws IOException {
        //when
        final ThrowableAssert.ThrowingCallable action = () -> {
            try (OutputCapture outer = OutputCapture.begin()) {
                try (OutputCapture inner = OutputCapture.begin()) {
                    outer.close();
                }
            }
        };
        //then
        assertThatThrownBy(action).isInstanceOf(IOException.class)
                                  .hasMessage("Not the current capture. Close the current capture first");
    }

    private String randomText() {
        return UUID.randomUUID()
                   .toString();
    }

}
