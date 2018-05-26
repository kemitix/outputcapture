package net.kemitix.outputcapture;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class StreamCopyFunctionTest {

    @Test
    public void requireSourceStream() {
        assertThatNullPointerException()
                .isThrownBy(() -> new StreamCopyFunction().apply(null))
                .withMessage("source");
    }

    @Test
    public void canCopyEmptySource() {
        //given
        final ByteArrayOutputStream source = new ByteArrayOutputStream(0);
        //when
        final ByteArrayOutputStream result = new StreamCopyFunction().apply(source);
        //then
        assertThat(result).isNotSameAs(source);
        assertThat(result.size()).isZero();
    }

    @Test
    public void canCopySingleCharSource() {
        //given
        final ByteArrayOutputStream source = new ByteArrayOutputStream();
        source.write('a');
        //when
        final ByteArrayOutputStream result = new StreamCopyFunction().apply(source);
        //then
        assertThat(result).isNotSameAs(source);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void canCopySource() {
        //given
        final ByteArrayOutputStream source = new ByteArrayOutputStream();
        final String string = "Longer string";
        new PrintStream(source).print(string);
        //when
        final ByteArrayOutputStream result = new StreamCopyFunction().apply(source);
        //then
        assertThat(result).isNotSameAs(source);
        assertThat(result.toString()).isEqualTo(string);
    }
}