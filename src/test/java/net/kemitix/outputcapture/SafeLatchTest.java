package net.kemitix.outputcapture;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class SafeLatchTest {

    @Test
    public void whenInterruptedThenInterruptHandlerIsCalled() {
        //given
        final Long maxAwaitMilliseconds = 100L;
        final AtomicBoolean interrupted = new AtomicBoolean(false);
        final SafeLatch safeLatch = new SafeLatch(1, maxAwaitMilliseconds, () -> interrupted.set(true));
        //when
        new Thread(() -> {
            final Thread thread = Thread.currentThread();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    thread.interrupt();
                }
            }).start();
            safeLatch.await();
        }).start();
        //then
        assertThat(interrupted).isTrue();
    }
}