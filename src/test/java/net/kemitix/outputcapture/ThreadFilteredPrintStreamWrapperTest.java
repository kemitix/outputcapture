package net.kemitix.outputcapture;

import net.kemitix.wrapper.Wrapper;
import net.kemitix.wrapper.printstream.PassthroughPrintStreamWrapper;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ThreadFilteredPrintStreamWrapper}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class ThreadFilteredPrintStreamWrapperTest {

    private ByteArrayOutputStream buffer;

    private Wrapper<PrintStream> printStream;

    @Before
    public void setUp() throws Exception {
        buffer = new ByteArrayOutputStream();
        printStream = new PassthroughPrintStreamWrapper(new PrintStream(buffer));
    }

    @Test
    public void canWriteByteWhenOnFilteredThread() {
        //given
        final Thread thread = Thread.currentThread();
        final ThreadFilteredPrintStreamWrapper filtered = new ThreadFilteredPrintStreamWrapper(printStream, thread);
        //when
        filtered.write('x');
        //then
        assertThat(buffer.toString()).isEqualTo("x");
    }

    @Test
    public void doesNotWriteByteWhenNotOnFilteredThread() {
        //given
        final Thread thread = new Thread();
        final ThreadFilteredPrintStreamWrapper filtered = new ThreadFilteredPrintStreamWrapper(printStream, thread);
        //when
        filtered.write('x');
        //then
        assertThat(buffer.toString()).isEqualTo("");
    }

    @Test
    public void canWriteByteArrayWhenOnFilteredThread() {
        //given
        final Thread thread = Thread.currentThread();
        final ThreadFilteredPrintStreamWrapper filtered = new ThreadFilteredPrintStreamWrapper(printStream, thread);
        //when
        filtered.write("test".getBytes(), 0, 4);
        //then
        assertThat(buffer.toString()).isEqualTo("test");
    }

    @Test
    public void doesNotWriteByteArrayWhenNotOnFilteredThread() {
        //given
        final Thread thread = new Thread();
        final ThreadFilteredPrintStreamWrapper filtered = new ThreadFilteredPrintStreamWrapper(printStream, thread);
        //when
        filtered.write("test".getBytes(), 0, 4);
        //then
        assertThat(buffer.toString()).isEqualTo("");
    }

    @Test
    public void canWriteByteWhenOnParentThread() {
        //given
        final Thread thread = new Thread();
        final ThreadFilteredPrintStreamWrapper filtered = new ThreadFilteredPrintStreamWrapper(printStream, thread);
        //when
        filtered.write('x', thread);
        //then
        assertThat(buffer.toString()).isEqualTo("x");
    }

    @Test
    public void doesNotWriteByteWhenNotOnParentThread() {
        //given
        final Thread thread = new Thread();
        final ThreadFilteredPrintStreamWrapper filtered = new ThreadFilteredPrintStreamWrapper(printStream, thread);
        final Thread otherThread = new Thread();
        //when
        filtered.write('x', otherThread);
        //then
        assertThat(buffer.toString()).isEqualTo("");
    }

    @Test
    public void canWriteByteArrayWhenOnParentThread() {
        //given
        final Thread thread = new Thread();
        final ThreadFilteredPrintStreamWrapper filtered = new ThreadFilteredPrintStreamWrapper(printStream, thread);
        //when
        filtered.write("test".getBytes(), 0, 4, thread);
        //then
        assertThat(buffer.toString()).isEqualTo("test");
    }

    @Test
    public void doesNotWriteByteArrayWhenNotOnParentThread() {
        //given
        final Thread thread = new Thread();
        final ThreadFilteredPrintStreamWrapper filtered = new ThreadFilteredPrintStreamWrapper(printStream, thread);
        final Thread otherThread = new Thread();
        //when
        filtered.write("test".getBytes(), 0, 4, otherThread);
        //then
        assertThat(buffer.toString()).isEqualTo("");
    }

}
