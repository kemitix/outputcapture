package net.kemitix.outputcapture;

import lombok.val;
import net.kemitix.wrapper.Wrapper;
import net.kemitix.wrapper.printstream.PassthroughPrintStreamWrapper;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssertAlternative;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private Wrapper<PrintStream> createdWrapper;

    private OutputStream captureTo;

    private PrintStream original;

    private Thread targetThread;

    private PrintStream wrapper;

    @Before
    public void setUp() {
        initMocks(this);
        copyRouter = new CopyRouter(wrapperFactory);
        captureTo = new ByteArrayOutputStream();
        original = new PrintStream(new ByteArrayOutputStream());
        targetThread = Thread.currentThread();
        wrapper = new PassthroughPrintStreamWrapper(original);
    }

    @Test
    public void wrapRequiresCaptureTo() {
        //given
        captureTo = null;
        //then
        assertThatWrapThrowsNullPointerException().withMessage("captureTo");
    }

    @Test
    public void wrapRequiresOriginalStream() {
        //given
        original = null;
        //then
        assertThatWrapThrowsNullPointerException().withMessage("originalStream");
    }

    @Test
    public void canWrapPrintStream() {
        //given
        given(wrapperFactory.copyPrintStream(eq(original), any())).willReturn(createdWrapper);
        //when
        val wrappingPrintStreams = copyRouter.wrap(captureTo, original, targetThread);
        //then
        Assertions.assertThat(wrappingPrintStreams.getMainWrapper())
                  .isSameAs(createdWrapper);
        Assertions.assertThat(wrappingPrintStreams.getOtherWrappers())
                  .isEmpty();
    }

    @Test
    public void canWrapWrapper() {
        //given
        given(wrapperFactory.copyPrintStream(eq(wrapper), any())).willReturn(createdWrapper);
        //when
        val wrappingPrintStreams = copyRouter.wrap(captureTo, wrapper, targetThread);
        //then
        Assertions.assertThat(wrappingPrintStreams.getMainWrapper())
                  .isSameAs(createdWrapper);
        Assertions.assertThat(wrappingPrintStreams.getOtherWrappers())
                  .isEmpty();
    }

    private ThrowableAssertAlternative<NullPointerException> assertThatWrapThrowsNullPointerException() {
        return Assertions.assertThatNullPointerException()
                         .isThrownBy(() -> copyRouter.wrap(captureTo, original, targetThread));
    }
}
