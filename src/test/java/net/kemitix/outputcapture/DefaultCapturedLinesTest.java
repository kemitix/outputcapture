package net.kemitix.outputcapture;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultCapturedLinesTest {

    @Test
    public void writeOutWithShortLineSeparator() {
        //given
        final DefaultCapturedLines capturedLines = new DefaultCapturedLines("X");
        final String input = "line 1Xline 2X";
        final List<Byte> byteList = new ArrayList<>();
        for (char aChar : input.toCharArray()) {
            byteList.add((byte) aChar);
        }
        //when
        byteList.forEach(capturedLines::writeOut);
        //then
        final List<String> output = capturedLines.stream()
                .map(CapturedOutputLine::asString)
                .collect(Collectors.toList());
        assertThat(output).containsExactly("line 1", "line 2");
    }

    @Test
    public void writeOutWithFullLineSeparator() {
        //given
        final DefaultCapturedLines capturedLines = new DefaultCapturedLines("XY");
        final String input = "line 1XYline 2XY";
        final List<Byte> byteList = new ArrayList<>();
        for (char aChar : input.toCharArray()) {
            byteList.add((byte) aChar);
        }
        //when
        byteList.forEach(capturedLines::writeOut);
        //then
        final List<String> output = capturedLines.stream()
                .map(CapturedOutputLine::asString)
                .collect(Collectors.toList());
        assertThat(output).containsExactly("line 1", "line 2");
    }

    @Test
    public void writeErrWithShortLineSeparator() {
        //given
        final DefaultCapturedLines capturedLines = new DefaultCapturedLines("X");
        final String input = "line 1Xline 2X";
        final List<Byte> byteList = new ArrayList<>();
        for (char aChar : input.toCharArray()) {
            byteList.add((byte) aChar);
        }
        //when
        byteList.forEach(capturedLines::writeErr);
        //then
        final List<String> output = capturedLines.stream()
                .map(CapturedOutputLine::asString)
                .collect(Collectors.toList());
        assertThat(output).containsExactly("line 1", "line 2");
    }

    @Test
    public void writeErrWithFullLineSeparator() {
        //given
        final DefaultCapturedLines capturedLines = new DefaultCapturedLines("XY");
        final String input = "line 1XYline 2XY";
        final List<Byte> byteList = new ArrayList<>();
        for (char aChar : input.toCharArray()) {
            byteList.add((byte) aChar);
        }
        //when
        byteList.forEach(capturedLines::writeErr);
        //then
        final List<String> output = capturedLines.stream()
                .map(CapturedOutputLine::asString)
                .collect(Collectors.toList());
        assertThat(output).containsExactly("line 1", "line 2");
    }

}