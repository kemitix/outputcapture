package net.kemitix.outputcapture;

import org.junit.Test;

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
    public void givenOneInterceptorWhenRemoveAllThenNoneLeft() {
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
    public void givenTwoInterceptorsWhenRemoveAllThenNoneLeft() {
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
        private OngoingCapturedOutput capture;

        MyAsyncCapture() {
            final SimpleLatch started = new SimpleLatch();
            capture = Captors.asyncCopyAll(maxAwaitMilliseconds).capture(() -> {
                started.countDown();
                finished.await();
            });
            started.await();
        }

        void remove() {
            finished.countDown();
            capture.join();
            assertThat(capture.executorIsShutdown()).isTrue();
        }

        boolean isComplete() {
            return capture.executorIsShutdown();
        }
    }

    private class SimpleLatch extends SafeLatch {
        SimpleLatch() {
            super(1, maxAwaitMilliseconds, () -> {});
        }
    }
}