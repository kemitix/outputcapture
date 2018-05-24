package net.kemitix.outputcapture;

import net.kemitix.conditional.Action;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class SafeLatchTest {

    @Test
    public void whenInterruptedThenInterruptHandlerIsCalled() {
        //given
        final Thread mainThread = Thread.currentThread();
        final Long maxAwaitMilliseconds = 100L;
        final AtomicBoolean interrupted = new AtomicBoolean(false);
        //when
        waitThen(10L, mainThread::interrupt);
        new SafeLatch(1, maxAwaitMilliseconds, () -> interrupted.set(true)).await();
        //then
        assertThat(interrupted).isTrue();
    }

    private static void waitThen(final long wait, final Action action) {
        new Thread(() -> {
            try {
                Thread.sleep(wait);
                action.perform();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}