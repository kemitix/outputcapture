package net.kemitix.outputcapture;

import net.kemitix.conditional.Action;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SafeLatchTest {

    @Test
    public void whenInterruptedThenWrapException() {
        //given
        final Thread mainThread = Thread.currentThread();
        //when
        waitThen(10L, mainThread::interrupt);
        //then
        assertThatThrownBy(() -> new SafeLatch(1).await())
                .isInstanceOf(OutputCaptureException.class);
    }

    private static void waitThen(final long wait, final Action action) {
        new Thread(() -> {
            try {
                Thread.sleep(10L);
                action.perform();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}