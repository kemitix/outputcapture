package net.kemitix.outputcapture;

import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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
    @Ignore("broken")
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
    @Ignore("broken")
    public void doesNotWriteByteArrayWhenNotOnFilteredThread() {
        //given
        final Thread thread = new Thread();
        final PrintStream filtered = WrapperFactory.threadFilteredPrintStream(printStream, thread);
        //when
        filtered.write("test".getBytes(), 0, 4);
        //then
        assertThat(buffer.toString()).isEqualTo("");
    }

    @Test(timeout = 200)
    @Ignore("broken")
    public void canWriteByteWhenOnParentThread() throws InterruptedException {
        //given
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<PrintStream> reference = new AtomicReference<>();
        final Thread thread = new Thread(() ->
                new Thread(() -> {
                    reference.get().write('x');
                    latch.countDown();
                }).start());
        final PrintStream filtered = WrapperFactory.threadFilteredPrintStream(printStream, thread);
        reference.set(filtered);
        //when
        thread.start();
        //then
        latch.await();
        assertThat(buffer.toString()).isEqualTo("x");
    }

    @Test(timeout = 200)
    @Ignore("broken")
    public void canWriteByteArrayWhenOnParentThread() throws InterruptedException {
        //given
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<PrintStream> reference = new AtomicReference<>();
        final Thread thread = new Thread(() ->
                new Thread(() -> {
                    reference.get().write("test".getBytes(), 0, 4);
                    latch.countDown();
                }).start());
        final PrintStream filtered = WrapperFactory.threadFilteredPrintStream(printStream, thread);
        reference.set(filtered);
        //when
        thread.start();
        //then
        latch.await();
        assertThat(buffer.toString()).isEqualTo("test");
    }
}
