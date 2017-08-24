package net.kemitix.outputcapture;

import net.kemitix.wrapper.Wrapper;
import net.kemitix.wrapper.printstream.CopyPrintStreamWrapper;
import net.kemitix.wrapper.printstream.RedirectPrintStreamWrapper;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WrapperFactoryImpl}.
 */
public class WrapperFactoryImplTest {

    @Test
    public void canCreateCopyPrintStreamWrapper() {
        //given
        final WrapperFactory wrapperFactory = new WrapperFactoryImpl();
        final PrintStream original = new PrintStream(new ByteArrayOutputStream());
        final PrintStream copyTo = new PrintStream(new ByteArrayOutputStream());
        //when
        final Wrapper<PrintStream> wrapper = wrapperFactory.copyPrintStream(original, copyTo);
        //then
        assertThat(wrapper).isInstanceOf(CopyPrintStreamWrapper.class)
                           .returns(original, Wrapper::getWrapperDelegate);
    }

    @Test
    public void canCreateRedirectPrintStreamWrapper() {
        //given
        final WrapperFactory wrapperFactory = new WrapperFactoryImpl();
        final PrintStream original = new PrintStream(new ByteArrayOutputStream());
        final PrintStream copyTo = new PrintStream(new ByteArrayOutputStream());
        //when
        final Wrapper<PrintStream> wrapper = wrapperFactory.redirectPrintStream(original, copyTo);
        //then
        assertThat(wrapper).isInstanceOf(RedirectPrintStreamWrapper.class)
                           .returns(original, Wrapper::getWrapperDelegate);
    }
}
