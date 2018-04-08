package net.kemitix.outputcapture;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractCapturedOutputTest {

    @Test
    public void whenOutputStreamHasLinesThenReturnLines() {
        //given
        final OutputStream outputStream = new ByteArrayOutputStream();
        new PrintStream(outputStream).printf("line 1%nline 2");
        final MyCapturedOutput capturedOutput = new MyCapturedOutput();
        //when
        final Stream<String> result = capturedOutput.asStream(outputStream);
        //then
        assertThat(result).containsExactly("line 1", "line 2");
    }

    @Test
    public void whenOutputStreamHasOneLineWithNoNewLineThenReturnOneLine() {
        //given
        final OutputStream outputStream = new ByteArrayOutputStream();
        new PrintStream(outputStream).print("line 1");
        final MyCapturedOutput capturedOutput = new MyCapturedOutput();
        //when
        final Stream<String> result = capturedOutput.asStream(outputStream);
        //then
        assertThat(result).containsExactly("line 1");
    }

    @Test
    public void whenOutputStreamHasOneLineWithNewLineThenReturnOneLine() {
        //given
        final OutputStream outputStream = new ByteArrayOutputStream();
        new PrintStream(outputStream).printf("line 1%n");
        final MyCapturedOutput capturedOutput = new MyCapturedOutput();
        //when
        final Stream<String> result = capturedOutput.asStream(outputStream);
        //then
        assertThat(result).containsExactly("line 1");
    }

    @Test
    public void whenOutputStreamHasNowLinesThenReturnEmptyStream() {
        //given
        final OutputStream outputStream = new ByteArrayOutputStream();
        final MyCapturedOutput capturedOutput = new MyCapturedOutput();
        //when
        final Stream<String> result = capturedOutput.asStream(outputStream);
        //then
        assertThat(result).isEmpty();
    }

    private class MyCapturedOutput extends AbstractCapturedOutput {
        @Override
        public Stream<String> getStdOut() {
            return null;
        }

        @Override
        public Stream<String> getStdErr() {
            return null;
        }

        @Override
        public ByteArrayOutputStream out() {
            return null;
        }

        @Override
        public ByteArrayOutputStream err() {
            return null;
        }

        @Override
        public Router getRouter() {
            return null;
        }
    }
}