package net.kemitix.outputcapture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests for {@link FilteredPrintStreamInterceptor}.
 */
public class FilteredPrintStreamInterceptorTest {

    @Mock
    private PrintStream printStream;

    @Captor
    private ArgumentCaptor<byte[]> writeCaptor;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void canCreateWithPrintStream() {
        //when
        final FilteredPrintStreamInterceptor interceptor =
                new FilteredPrintStreamInterceptor(printStream, o -> true);
        //then
        assertThat(interceptor.getPrintStream()).isSameAs(printStream);
    }

    @Test
    public void canCreateWithPrintStreamInterceptor() {
        //given
        final PassthroughPrintStreamInterceptor existing =
                new PassthroughPrintStreamInterceptor(printStream);
        //when
        final FilteredPrintStreamInterceptor interceptor =
                new FilteredPrintStreamInterceptor(((PrintStreamInterceptor) existing), o -> true);
        //then
        assertThat(interceptor.getPrintStream()).isSameAs(printStream);
        assertThat(interceptor.getWrappedInterceptor()).contains(existing);
    }

    @Test
    public void whenPredicateTrueThenFilterPrintsLine() throws IOException {
        //given
        final Predicate<String> predicate = "test"::equals;
        final FilteredPrintStreamInterceptor interceptor = new FilteredPrintStreamInterceptor(printStream, predicate);
        //when
        interceptor.println("test");
        //then
        then(printStream).should()
                         .write(writeCaptor.capture(), eq(0), eq(4));
        assertThat(writeCaptor.getValue()).contains(new byte[]{'t', 'e', 's', 't', '\n'});
    }

    @Test
    public void whenPredicateTrueThenFilterPrintsString() throws IOException {
        //given
        final Predicate<String> predicate = "test"::equals;
        final FilteredPrintStreamInterceptor interceptor = new FilteredPrintStreamInterceptor(printStream, predicate);
        //when
        interceptor.print("test");
        //then
        then(printStream).should()
                         .write(writeCaptor.capture(), eq(0), eq(4));
        assertThat(writeCaptor.getValue()).contains(new byte[]{'t', 'e', 's', 't'});
    }

    @Test
    public void whenPredicateFalseThenFilterIgnoresPrint() {
        //given
        final Predicate<String> predicate = "garbage"::equals;
        final FilteredPrintStreamInterceptor interceptor = new FilteredPrintStreamInterceptor(printStream, predicate);
        //when
        assertThatCode(() -> interceptor.print("test")).doesNotThrowAnyException();
        //then
        then(printStream).shouldHaveZeroInteractions();
    }

    @Test
    public void whenPredicateFalseThenFilterIgnoresPrintln() {
        //given
        final Predicate<String> predicate = "garbage"::equals;
        final FilteredPrintStreamInterceptor interceptor = new FilteredPrintStreamInterceptor(printStream, predicate);
        //when
        assertThatCode(() -> interceptor.println("test")).doesNotThrowAnyException();
        //then
        then(printStream).shouldHaveZeroInteractions();
    }

    @Test
    public void predicateIgnoresDirectWrites() {
        //given
        final FilteredPrintStreamInterceptor interceptor = new FilteredPrintStreamInterceptor(printStream, o -> false);
        //when
        assertThatCode(() -> interceptor.write('x')).doesNotThrowAnyException();
        //then
        then(printStream).should()
                         .write('x');
    }
}
