package net.kemitix.outputcapture;

import lombok.val;
import net.kemitix.wrapper.Wrapper;
import net.kemitix.wrapper.printstream.CopyPrintStreamWrapper;
import net.kemitix.wrapper.printstream.PassthroughPrintStreamWrapper;
import net.kemitix.wrapper.printstream.RedirectPrintStreamWrapper;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WrapperFactoryImpl}.
 */
public class WrapperFactoryImplTest {

    private WrapperFactory wrapperFactory;

    private PrintStream original;

    private PrintStream copyTo;

    private Wrapper<PrintStream> wrapper;

    @Before
    public void setUp() throws Exception {
        wrapperFactory = new WrapperFactoryImpl();
        original = new PrintStream(new ByteArrayOutputStream());
        copyTo = new PrintStream(new ByteArrayOutputStream());
        wrapper = new PassthroughPrintStreamWrapper(new PrintStream(new ByteArrayOutputStream()));
    }

    @Test
    public void canCreateCopyPrintStreamWrapperForPrintStream() {
        //when
        val result = wrapperFactory.copyPrintStream(original, copyTo);
        //then
        assertThat(result).isInstanceOf(CopyPrintStreamWrapper.class)
                           .returns(original, Wrapper::getWrapperDelegate);
    }

    @Test
    public void canCreateRedirectPrintStreamWrapperForPrintStream() {
        //when
        val result = wrapperFactory.redirectPrintStream(original, copyTo);
        //then
        assertThat(result).isInstanceOf(RedirectPrintStreamWrapper.class)
                           .returns(original, Wrapper::getWrapperDelegate);
    }

    @Test
    public void canCreateRedirectPrintStreamWrapperForWrapper() {
        //when
        val result = wrapperFactory.redirectPrintStream(wrapper, copyTo);
        //then
        assertThat(result).isInstanceOf(RedirectPrintStreamWrapper.class)
                           .returns(wrapper, Wrapper::getWrapperDelegate);
    }
}
