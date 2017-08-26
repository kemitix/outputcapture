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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests for {@link RedirectRouter}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class RedirectRouterTest {

    @Mock
    private WrapperFactory wrapperFactory;

    private Router router;

    @Mock
    private Wrapper<PrintStream> createdWrapper;

    private Wrapper<PrintStream> wrapper;

    private OutputStream captureTo;

    private OutputStream originalOutput;

    private PrintStream original;

    private Thread targetThread;

    @Before
    public void setUp() {
        initMocks(this);
        originalOutput = new ByteArrayOutputStream();
        original = new PrintStream(originalOutput);
        captureTo = new ByteArrayOutputStream();
        wrapper = new PassthroughPrintStreamWrapper(original);
        router = new RedirectRouter(wrapperFactory);
        targetThread = Thread.currentThread();
    }

    @Test
    public void wrapRequiresCaptureTo() {
        //given
        captureTo = null;
        //then
        assertThatRouterWrapThrowsNullPointerException().withMessage("captureTo");
    }

    @Test
    public void wrapRequiresOriginalStream() {
        //given
        original = null;
        //then
        assertThatRouterWrapThrowsNullPointerException().withMessage("originalStream");
    }

    @Test
    public void canWrapPrintStream() {
        //given
        given(wrapperFactory.redirectPrintStream(eq(original), any())).willReturn(createdWrapper);
        //when
        val wrappingPrintStreams = router.wrap(captureTo, original, targetThread);
        //then
        assertThat(wrappingPrintStreams.getMainWrapper()).isSameAs(createdWrapper);
        assertThat(wrappingPrintStreams.getOtherWrappers()).isEmpty();
    }

    @Test
    public void canWrapWrapper() {
        //given
        given(wrapperFactory.redirectPrintStream(eq(wrapper), any())).willReturn(createdWrapper);
        //when
        val wrappingPrintStreams = router.wrap(captureTo, wrapper.asCore(), targetThread);
        //then
        assertThat(wrappingPrintStreams.getMainWrapper()).isSameAs(createdWrapper);
        assertThat(wrappingPrintStreams.getOtherWrappers()).isEmpty();
    }

    private ThrowableAssertAlternative<NullPointerException> assertThatRouterWrapThrowsNullPointerException() {
        return assertThatNullPointerException().isThrownBy(() -> router.wrap(captureTo, original, targetThread));
    }
}
