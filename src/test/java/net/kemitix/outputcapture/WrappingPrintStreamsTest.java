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
        assertThatNullPointerException().isThrownBy(() -> new WrappingPrintStreams(null))
                                        .withMessage("mainWrapper");
    }

    @Test
    public void canCreateWithSingleWrapper() {
        //given
        val wrapper = wrapper();
        //when
        val result = new WrappingPrintStreams(wrapper);
        //then
        assertThat(result).returns(wrapper, WrappingPrintStreams::getMainWrapper);
        assertThat(result.getOtherWrappers()).isEmpty();
    }

    @Test
    public void canCreateWithTwoWrappers() {
        //given
        val wrapper1 = wrapper();
        val wrapper2 = wrapper();
        //when
        val result = new WrappingPrintStreams(wrapper1, wrapper2);
        //then
        assertThat(result).returns(wrapper1, WrappingPrintStreams::getMainWrapper);
        assertThat(result.getOtherWrappers()).containsExactly(wrapper2);
    }

    @Test
    public void canCreateWithThreeWrappers() {
        //given
        val wrapper1 = wrapper();
        val wrapper2 = wrapper();
        val wrapper3 = wrapper();
        //when
        val result = new WrappingPrintStreams(wrapper1, wrapper2, wrapper3);
        //then
        assertThat(result).returns(wrapper1, WrappingPrintStreams::getMainWrapper);
        assertThat(result.getOtherWrappers()).containsExactly(wrapper2, wrapper3);
    }

    private Wrapper<PrintStream> wrapper() {
        return () -> new WrapperState<>(new PrintStream(new ByteArrayOutputStream()));
    }
}
