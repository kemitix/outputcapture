package net.kemitix.outputcapture;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class SafeLatchTest {

    @Test
    public void whenInterruptedThenInterruptHandlerIsCalled() {
        //given
        final Long maxAwaitMilliseconds = 100L;
        final AtomicBoolean interrupted = new AtomicBoolean(false);
        final SafeLatch safeLatch = new SafeLatch(1, maxAwaitMilliseconds, () -> interrupted.set(true));
        final CountDownLatch latch = new CountDownLatch(1);
        //when
        new Thread(() -> {
            final Thread thread = Thread.currentThread();
            waitThenInterrupt(thread, maxAwaitMilliseconds, latch);
            latch.countDown();
            safeLatch.await();
        }).start();
        //then
        assertThat(interrupted).isTrue();
    }

    private void waitThenInterrupt(final Thread thread, final long millis, final CountDownLatch latch) {
        new Thread(() -> {
            try {
                latch.await(millis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // ignore
            }
            thread.interrupt();
        }).start();
    }
}