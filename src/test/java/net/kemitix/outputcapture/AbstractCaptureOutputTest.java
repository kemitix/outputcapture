package net.kemitix.outputcapture;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Assumptions;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractCaptureOutputTest {

    private final Long maxAwaitMilliseconds = 100L;

    @Test
    public void givenNoInteceptorsWhenRemoveAllThenNoneLeft() {
        //given
        //when
        AbstractCaptureOutput.removeAllActiveCaptures();
        //then
        assertThat(AbstractCaptureOutput.activeCount()).isZero();
    }

    @Test
    public void givenOneInterceptorWhenRemoveAllThenNoneLeft() throws InterruptedException {
        //given
        final MyAsyncCapture capture = new MyAsyncCapture();
        assertThat(AbstractCaptureOutput.activeCount()).isNotZero();
        //when
        AbstractCaptureOutput.removeAllActiveCaptures();
        //then
        capture.remove();
        assertThat(AbstractCaptureOutput.activeCount()).isZero();
        assertThat(capture.isComplete()).isTrue();
    }

    @Test
    public void givenTwoInterceptorsWhenRemoveAllThenNoneLeft() throws InterruptedException {
        //given
        final MyAsyncCapture capture1 = new MyAsyncCapture();
        final MyAsyncCapture capture2 = new MyAsyncCapture();
        assertThat(AbstractCaptureOutput.activeCount()).isNotZero();
        //when
        AbstractCaptureOutput.removeAllActiveCaptures();
        //then
        capture1.remove();
        capture2.remove();
        assertThat(AbstractCaptureOutput.activeCount()).isZero();
        assertThat(capture1.isComplete()).isTrue();
        assertThat(capture2.isComplete()).isTrue();
    }

    private class MyAsyncCapture {

        private SimpleLatch finished = new SimpleLatch();
        private CountDownLatch completed;
        private OngoingCapturedOutput capture;

        MyAsyncCapture() throws InterruptedException {
            final CountDownLatch started = new SimpleLatch();
            capture = Captors.asyncCopyAll(maxAwaitMilliseconds).capture(() -> {
                release(started);
                await(finished);
            });
            await(started);
            completed = capture.getCompletedLatch();
        }

        void remove() throws InterruptedException {
            finished.countDown();
            await(completed);
            assertThat(capture.executorIsShutdown()).isTrue();
        }

        boolean isComplete() {
            return capture.executorIsShutdown();
        }
    }

    private void release(final CountDownLatch latch) {
        latch.countDown();
    }

    private void await(final CountDownLatch latch) throws InterruptedException {
        latch.await(1, TimeUnit.SECONDS);
    }

    private class SimpleLatch extends CountDownLatch {
        SimpleLatch() {
            super(1);
        }
    }
}