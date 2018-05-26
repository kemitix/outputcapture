package net.kemitix.outputcapture;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class SafeLatchTest {

    @Test(timeout = 300L)
    public void whenInterruptedThenInterruptHandlerIsCalled() throws InterruptedException {
        //given
        final Long longWait = 100L;
        final Long shortWait = 50L;
        final AtomicBoolean interrupted = new AtomicBoolean(false);
        final SafeLatch latchToBeInterrupted = new SafeLatch(1, longWait, () -> interrupted.set(true));
        final CountDownLatch testThreadFinished = new CountDownLatch(1);
        //when
        final Thread testThread = new Thread(() -> {
            // this it the thread that will experience an interruption
            final Thread currentThread = Thread.currentThread();
            // start another thread that will interrupt the current thread after a short wait
            final Thread interruptThread = waitThenInterruptThread(shortWait, currentThread);
            interruptThread.start();
            latchToBeInterrupted.await();
            testThreadFinished.countDown();
        });
        testThread.start();
        //then
        testThreadFinished.await();
        assertThat(interrupted).isTrue();
    }

    private Thread waitThenInterruptThread(
            final Long timeToWait,
            final Thread thread
    ) {
        return new Thread(() -> {
            try {
                new CountDownLatch(1).await(timeToWait, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // ignore
            }
            thread.interrupt();
        });
    }

    @Test
    public void whenTimesOutThenIsTimedOut() {
        //given
        final Long shortWait = 50L;
        final SafeLatch latch = new SafeLatch(1, shortWait, () -> {
        });
        //when
        latch.await();
        //then
        assertThat(latch.isTimedout()).isTrue();
    }

    @Test
    public void whenNotTimedOutThenIsNotTimedOut() {
        //given
        final Long shortWait = 50L;
        final SafeLatch latch = new SafeLatch(1, shortWait, () -> {
        });
        latch.countDown();
        //when
        latch.await();
        //then
        assertThat(latch.isTimedout()).isFalse();
    }

}