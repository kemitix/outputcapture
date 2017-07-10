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

import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for capturing output.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class CaptureTest {

    @Test
    public void canCaptureSystemOut() {
        //given
        final String line1 = randomText();
        final String line2 = randomText();
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
        final String line1 = randomText();
        final String line2 = randomText();
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
        final String capturedText = randomText();
        final String nonCapturedText = randomText();
        final OutputCapture originalCapture = OutputCapture.begin();
        final OutputCapture capture = OutputCapture.begin();
        System.out.println(capturedText);
        //when
        capture.close();
        System.out.println(nonCapturedText);
        //then
        assertThat(capture.getStdOut()).containsExactly(capturedText)
                                       .doesNotContain(nonCapturedText);
        assertThat(originalCapture.getStdOut()).containsExactly(nonCapturedText)
                                               .doesNotContain(capturedText);
    }

    @Test
    public void canRestoreNormalSystemErr() throws Exception {
        //given
        final String capturedText = randomText();
        final String nonCapturedText = randomText();
        final OutputCapture originalCapture = OutputCapture.begin();
        final OutputCapture capture = OutputCapture.begin();
        System.err.println(capturedText);
        //when
        capture.close();
        System.err.println(nonCapturedText);
        //then
        assertThat(capture.getStdErr()).containsExactly(capturedText)
                                       .doesNotContain(nonCapturedText);
        assertThat(originalCapture.getStdErr()).containsExactly(nonCapturedText)
                                               .doesNotContain(capturedText);
    }

    @Test
    public void canClearCapturedOutput() {
        //given
        final String line1 = randomText();
        final String line2 = randomText();
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
        final String line1 = randomText();
        final String line2 = randomText();
        final OutputCapture capture = OutputCapture.begin();
        //when
        System.err.println(line1);
        capture.clear();
        System.err.println(line2);
        //then
        assertThat(capture.getStdErr()).containsExactly(line2)
                                       .doesNotContain(line1);
    }

    private String randomText() {
        return UUID.randomUUID()
                   .toString();
    }

}
