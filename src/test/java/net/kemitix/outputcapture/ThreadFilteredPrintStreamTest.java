package net.kemitix.outputcapture;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ThreadFilteredPrintStream}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class ThreadFilteredPrintStreamTest {

    private ByteArrayOutputStream buffer;

    private PrintStream printStream;

    @Before
    public void setUp() throws Exception {
        buffer = new ByteArrayOutputStream();
        printStream = new PrintStream(buffer);
    }

    @Test
    public void canWriteByteWhenOnFilteredThread() {
        //given
        final Thread thread = Thread.currentThread();
        final ThreadFilteredPrintStream filtered = new ThreadFilteredPrintStream(printStream, thread);
        //when
        filtered.write('x');
        //then
        assertThat(buffer.toString()).isEqualTo("x");
    }

    @Test
    public void doesNotWriteByteWhenNotOnFilteredThread() {
        //given
        final Thread thread = new Thread();
        final ThreadFilteredPrintStream filtered = new ThreadFilteredPrintStream(printStream, thread);
        //when
        filtered.write('x');
        //then
        assertThat(buffer.toString()).isEqualTo("");
    }

    @Test
    public void canWriteByteArrayWhenOnFilteredThread() {
        //given
        final Thread thread = Thread.currentThread();
        final ThreadFilteredPrintStream filtered = new ThreadFilteredPrintStream(printStream, thread);
        //when
        filtered.write("test".getBytes(), 0, 4);
        //then
        assertThat(buffer.toString()).isEqualTo("test");
    }

    @Test
    public void doesNotWriteByteArrayWhenNotOnFilteredThread() {
        //given
        final Thread thread = new Thread();
        final ThreadFilteredPrintStream filtered = new ThreadFilteredPrintStream(printStream, thread);
        //when
        filtered.write("test".getBytes(), 0, 4);
        //then
        assertThat(buffer.toString()).isEqualTo("");
    }

    @Test
    public void canWriteByteWhenOnParentThread() {
        //given
        final Thread thread = new Thread();
        final ThreadFilteredPrintStream filtered = new ThreadFilteredPrintStream(printStream, thread);
        //when
        filtered.write('x', thread);
        //then
        assertThat(buffer.toString()).isEqualTo("x");
    }

    @Test
    public void doesNotWriteByteWhenNotOnParentThread() {
        //given
        final Thread thread = new Thread();
        final ThreadFilteredPrintStream filtered = new ThreadFilteredPrintStream(printStream, thread);
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
        final ThreadFilteredPrintStream filtered = new ThreadFilteredPrintStream(printStream, thread);
        //when
        filtered.write("test".getBytes(), 0, 4, thread);
        //then
        assertThat(buffer.toString()).isEqualTo("test");
    }

    @Test
    public void doesNotWriteByteArrayWhenNotOnParentThread() {
        //given
        final Thread thread = new Thread();
        final ThreadFilteredPrintStream filtered = new ThreadFilteredPrintStream(printStream, thread);
        final Thread otherThread = new Thread();
        //when
        filtered.write("test".getBytes(), 0, 4, otherThread);
        //then
        assertThat(buffer.toString()).isEqualTo("");
    }

}
