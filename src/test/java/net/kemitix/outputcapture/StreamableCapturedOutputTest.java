package net.kemitix.outputcapture;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamableCapturedOutputTest {

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
    public void whenOutputStreamHasNoLinesThenReturnEmptyStream() {
        //given
        final OutputStream outputStream = new ByteArrayOutputStream();
        final MyCapturedOutput capturedOutput = new MyCapturedOutput();
        //when
        final Stream<String> result = capturedOutput.asStream(outputStream);
        //then
        assertThat(result).isEmpty();
    }

    private class MyCapturedOutput implements StreamableCapturedOutput {
    }
}