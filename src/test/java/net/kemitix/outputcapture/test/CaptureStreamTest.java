package net.kemitix.outputcapture.test;

import net.kemitix.outputcapture.CaptureOutput;
import net.kemitix.outputcapture.CapturedOutputLine;
import net.kemitix.outputcapture.ThrowingCallable;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class CaptureStreamTest {

    private final String line1Out = "line 1 out";
    private final String line1Err = "line 1 err";
    private final String line2Out = "line 2 out";
    private final String line2Err = "line 2 err";

    @Test
    public void canFilterToStdOut() {
        //when
        final List<String> lines =
                CaptureOutput.of(writeOutput())
                        .stream()
                        .filter(CapturedOutputLine::isOut)
                        .map(CapturedOutputLine::asString)
                        .collect(Collectors.toList());
        //then
        assertThat(lines).containsExactly(line1Out, line2Out);
    }

    @Test
    public void canFilterToStdErr() {
        //when
        final List<String> lines =
                CaptureOutput.of(writeOutput())
                        .stream()
                        .filter(CapturedOutputLine::isErr)
                        .map(CapturedOutputLine::asString)
                        .collect(Collectors.toList());
        //then
        assertThat(lines).containsExactly(line1Err, line2Err);
    }

    @Test
    public void canStreamOutWhenCapturingAsync() {
        //when
        final List<String> lines =
                CaptureOutput.ofThread(writeOutput(), 100L)
                        .stream()
                        .filter(CapturedOutputLine::isOut)
                        .map(CapturedOutputLine::asString)
                        .collect(Collectors.toList());
        //then
        assertThat(lines).containsExactly(line1Out, line2Out);
    }

    @Test
    public void canStreamErrWhenCapturingAsync() {
        //when
        final List<String> lines =
                CaptureOutput.ofThread(writeOutput(), 100L)
                        .stream()
                        .filter(CapturedOutputLine::isErr)
                        .map(CapturedOutputLine::asString)
                        .collect(Collectors.toList());
        //then
        assertThat(lines).containsExactly(line1Err, line2Err);
    }

    private ThrowingCallable writeOutput() {
        return () -> {
            System.out.println(line1Out);
            System.err.println(line1Err);
            System.out.println(line2Out);
            System.err.println(line2Err);
        };
    }
}
