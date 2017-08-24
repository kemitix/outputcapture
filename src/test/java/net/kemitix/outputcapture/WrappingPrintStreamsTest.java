package net.kemitix.outputcapture;

import lombok.val;
import net.kemitix.wrapper.Wrapper;
import net.kemitix.wrapper.WrapperState;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WrappingPrintStreamsTest {

    @Test
    public void requiresAWrapper() {
        //given
        final Wrapper<PrintStream> wrapper = null;
        //then
        Assertions.assertThatNullPointerException()
                  .isThrownBy(() ->
                                      //when
                                      new WrappingPrintStreams(wrapper))
                  //and
                  .withMessage("mainWrapper");
    }

    @Test
    public void canCreateWithSingleWrapper() {
        //given
        final Wrapper<PrintStream> wrapper =
                () -> new WrapperState<>(new PrintStream(new ByteArrayOutputStream()));
        //when
        final WrappingPrintStreams result = new WrappingPrintStreams(wrapper);
        //then
        assertThat(result).returns(wrapper, WrappingPrintStreams::getMainWrapper);
        assertThat(result.getOtherWrappers()).isEmpty();
    }

    @Test
    public void canCreateWithTwoWrappers() {
        //given
        final Wrapper<PrintStream> wrapper1 =
                () -> new WrapperState<>(new PrintStream(new ByteArrayOutputStream()));
        final Wrapper<PrintStream> wrapper2 =
                () -> new WrapperState<>(new PrintStream(new ByteArrayOutputStream()));
        //when
        final WrappingPrintStreams result = new WrappingPrintStreams(wrapper1, wrapper2);
        //then
        assertThat(result).returns(wrapper1, WrappingPrintStreams::getMainWrapper);
        assertThat(result.getOtherWrappers()).containsExactly(wrapper2);
    }

    @Test
    public void canCreateWithThreeWrappers() {
        //given
        final Wrapper<PrintStream> wrapper1 =
                () -> new WrapperState<>(new PrintStream(new ByteArrayOutputStream()));
        final Wrapper<PrintStream> wrapper2 =
                () -> new WrapperState<>(new PrintStream(new ByteArrayOutputStream()));
        final Wrapper<PrintStream> wrapper3 =
                () -> new WrapperState<>(new PrintStream(new ByteArrayOutputStream()));
        //when
        final WrappingPrintStreams result = new WrappingPrintStreams(wrapper1, wrapper2, wrapper3);
        //then
        assertThat(result).returns(wrapper1, WrappingPrintStreams::getMainWrapper);
        assertThat(result.getOtherWrappers()).containsExactly(wrapper2, wrapper3);
    }
}
