package net.kemitix.outputcapture;

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

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private PrintStream printStream = new PrintStream(buffer);

    @Test
    public void canWriteByteWhenOnFilteredThread() {
        //given
        final Thread thread = Thread.currentThread();
        final PrintStream filtered = WrapperFactory.threadFilteredPrintStream(printStream, thread);
        //when
        filtered.write('x');
        //then
        assertThat(buffer.toString()).isEqualTo("x");
    }

    @Test
    public void doesNotWriteByteWhenNotOnFilteredThread() {
        //given
        final Thread thread = new Thread();
        final PrintStream filtered = WrapperFactory.threadFilteredPrintStream(printStream, thread);
        //when
        filtered.write('x');
        //then
        assertThat(buffer.toString()).isEmpty();
    }

    @Test
    public void canWriteByteArrayWhenOnFilteredThread() {
        //given
        final Thread thread = Thread.currentThread();
        final PrintStream filtered = WrapperFactory.threadFilteredPrintStream(printStream, thread);
        //when
        filtered.write("test".getBytes(), 0, 4);
        //then
        assertThat(buffer.toString()).isEqualTo("test");
    }

    @Test
    public void doesNotWriteByteArrayWhenNotOnFilteredThread() {
        //given
        final Thread thread = new Thread();
        final PrintStream filtered = WrapperFactory.threadFilteredPrintStream(printStream, thread);
        //when
        filtered.write("test".getBytes(), 0, 4);
        //then
        assertThat(buffer.toString()).isEqualTo("");
    }

    @Test
    public void canWriteByteWhenOnParentThread() {
        //given
        final Thread thread = new Thread();
        final PrintStream filtered = WrapperFactory.threadFilteredPrintStream(printStream, thread);
        //when
        filtered.write('x');
        //then
        assertThat(buffer.toString()).isEqualTo("x");
    }

    @Test
    public void doesNotWriteByteWhenNotOnParentThread() {
        //given
        final Thread thread = new Thread();
        final PrintStream filtered = WrapperFactory.threadFilteredPrintStream(printStream, thread);
        //when
        filtered.write('x');
        //then
        assertThat(buffer.toString()).isEqualTo("");
    }

    @Test
    public void canWriteByteArrayWhenOnParentThread() {
        //given
        final Thread thread = new Thread();
        final PrintStream filtered = WrapperFactory.threadFilteredPrintStream(printStream, thread);
        //when
        filtered.write("test".getBytes(), 0, 4);
        //then
        assertThat(buffer.toString()).isEqualTo("test");
    }

    @Test
    public void doesNotWriteByteArrayWhenNotOnParentThread() {
        //given
        final Thread thread = new Thread();
        final PrintStream filtered = WrapperFactory.threadFilteredPrintStream(printStream, thread);
        //when
        filtered.write("test".getBytes(), 0, 4);
        //then
        assertThat(buffer.toString()).isEqualTo("");
    }

}
