package net.kemitix.outputcapture;

import lombok.val;
import net.kemitix.wrapper.Wrapper;
import net.kemitix.wrapper.printstream.CopyPrintStreamWrapper;
import net.kemitix.wrapper.printstream.PassthroughPrintStreamWrapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests for {@link CopyRouter}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class CopyRouterTest {

    @Mock
    private WrapperFactory wrapperFactory;

    private CopyRouter copyRouter;

    @Mock
    private Wrapper<PrintStream> wrapper;

    @Before
    public void setUp() {
        initMocks(this);
        copyRouter = new CopyRouter(wrapperFactory);
    }

    @Test
    public void wrapRequiresCaptureTo() {
        //given
        final OutputStream captureTo = null;
        final PrintStream original = new PrintStream(new ByteArrayOutputStream());
        final Thread targetThread = Thread.currentThread();
        //then
        Assertions.assertThatNullPointerException()
                  .isThrownBy(() ->
                                      //when
                                      copyRouter.wrap(captureTo, original, targetThread))
                  //and
                  .withMessage("captureTo");
    }

    @Test
    public void wrapRequiresOriginalStream() {
        //given
        final OutputStream captureTo = new ByteArrayOutputStream();
        final PrintStream original = null;
        final Thread targetThread = Thread.currentThread();
        //then
        Assertions.assertThatNullPointerException()
                  .isThrownBy(() ->
                                      //when
                                      copyRouter.wrap(captureTo, original, targetThread))
                  //and
                  .withMessage("originalStream");
    }

    @Test
    public void canWrapPrintStream() {
        //given
        final OutputStream captureTo = new ByteArrayOutputStream();
        final PrintStream original = new PrintStream(new ByteArrayOutputStream());
        final Thread targetThread = Thread.currentThread();
        given(wrapperFactory.copyPrintStream(any(), any())).willReturn(wrapper);
        //when
        val wrappingPrintStreams = copyRouter.wrap(captureTo, original, targetThread);
        //then
        Assertions.assertThat(wrappingPrintStreams.getMainWrapper())
                  .isSameAs(wrapper);
        Assertions.assertThat(wrappingPrintStreams.getOtherWrappers())
                  .isEmpty();
    }

    @Test
    public void canWrapWrapper() {
        //given
        final OutputStream captureTo = new ByteArrayOutputStream();
        final PrintStream original = new PrintStream(new ByteArrayOutputStream());
        final PrintStream wrapper = new PassthroughPrintStreamWrapper(original);
        final Thread targetThread = Thread.currentThread();
        //when
        val wrappingPrintStreams = copyRouter.wrap(captureTo, wrapper, targetThread);
        //then
        Assertions.assertThat(wrappingPrintStreams.getMainWrapper())
                  .isInstanceOf(CopyPrintStreamWrapper.class)
                  .returns(wrapper, Wrapper::getWrapperDelegate);
        Assertions.assertThat(wrappingPrintStreams.getOtherWrappers())
                  .isEmpty();
    }

}
