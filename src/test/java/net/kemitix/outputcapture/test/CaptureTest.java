package net.kemitix.outputcapture.test;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import net.kemitix.outputcapture.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(HierarchicalContextRunner.class)
public class CaptureTest {

    //<editor-fold desc="fields">
    private static final String STARTING_OUT = "starting out";
    private static final String STARTING_ERR = "starting err";
    private static final String FINISHED_OUT = "finished out";
    private static final String FINISHED_ERR = "finished err";

    private final Long maxAwaitMilliseconds = 100L;
    private final String line1 = "1:" + UUID.randomUUID().toString();
    private final String line2 = "2:" + UUID.randomUUID().toString();
    //</editor-fold>

    @Rule
    public Timeout globalTimeout = Timeout.seconds(100L);


    @Before
    public void setUp() {
        assertThat(CaptureOutput.activeCount()).as("No existing captures").isZero();
    }

    @After
    public void tearDown() {
        final int activeCount = CaptureOutput.activeCount();
        CaptureOutput.removeAllInterceptors();
        assertThat(activeCount).as("All captures removed").isZero();
    }

    public class Synchronous {

        public class ThreadFiltered {

            // of
            public class Redirect {

                public class CaptureSystem {

                    @Test
                    public void out() {
                        //when
                        final CapturedOutput captured = CaptureOutput.of(() -> {
                            writeOutput(System.out, line1, line2);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput captured = CaptureOutput.of(() -> {
                            writeOutput(System.err, line1, line2);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(captured.getStdErr()).containsExactly(line1, line2);
                    }

                }

                public class ReplaceSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();
                    private final AtomicReference<PrintStream> replacement = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        CaptureOutput.of(() -> replacement.set(System.out), maxAwaitMilliseconds);
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.of(() -> replacement.set(System.err), maxAwaitMilliseconds);
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }
                }

                public class RestoreSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        CaptureOutput.of(() -> {
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(System.out).isSameAs(original.get());
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.of(() -> {
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(System.err).isSameAs(original.get());
                    }
                }

                public class ExceptionThrown {

                    private final UnsupportedOperationException cause = new UnsupportedOperationException(line1);

                    @Test
                    public void wrappedInOutputCaptureException() {
                        //then
                        assertThatThrownBy(() -> CaptureOutput.of(() -> {
                            throw cause;
                        }, maxAwaitMilliseconds))
                                .isInstanceOf(OutputCaptureException.class)
                                .hasCause(cause);
                    }
                }

                public class FiltersToTargetThread {

                    private final ExecutorService catchMe = Executors.newSingleThreadExecutor();
                    private final ExecutorService ignoreMe = Executors.newSingleThreadExecutor();
                    private final AtomicReference<CapturedOutput> reference = new AtomicReference<>();
                    private final SafeLatch ready = createLatch();
                    private final SafeLatch done = createLatch();
                    private final SafeLatch finished = createLatch();

                    @Test
                    public void out() {
                        //when
                        catchMe.submit(() -> captureThis(ready, done, reference, finished, catchMe));
                        ignoreMe.submit(() -> ignoreThis(ready, done, ignoreMe));
                        //then
                        awaitLatch(finished);
                        final CapturedOutput capturedOutput = reference.get();
                        assertThat(capturedOutput.getStdOut()).containsExactly(STARTING_OUT, FINISHED_OUT);
                    }

                    @Test
                    public void err() {
                        //when
                        catchMe.submit(() -> captureThis(ready, done, reference, finished, catchMe));
                        ignoreMe.submit(() -> ignoreThis(ready, done, ignoreMe));
                        //then
                        awaitLatch(finished);
                        final CapturedOutput capturedOutput = reference.get();
                        assertThat(capturedOutput.getStdErr()).containsExactly(STARTING_ERR, FINISHED_ERR);
                    }

                    private void captureThis(
                            final SafeLatch ready,
                            final SafeLatch done,
                            final AtomicReference<CapturedOutput> reference,
                            final SafeLatch finished,
                            final ExecutorService catchMe
                    ) {
                        final CapturedOutput capturedOutput =
                                CaptureOutput.of(() -> asyncWithInterrupt(ready, done), maxAwaitMilliseconds);
                        reference.set(capturedOutput);
                        releaseLatch(finished);
                        catchMe.shutdown();
                    }

                    private void ignoreThis(
                            final SafeLatch ready,
                            final SafeLatch done,
                            final ExecutorService ignoreMe
                    ) {
                        awaitLatch(ready);
                        System.out.println("ignore me");
                        releaseLatch(done);
                        ignoreMe.shutdown();
                    }
                }

                public class RedirectFromOriginal {

                    private final AtomicReference<CapturedOutput> ref = new AtomicReference<>();

                    @Test
                    public void out() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final CapturedOutput copyOf = CaptureOutput.of(() -> {
                                writeOutput(System.out, line1, line2);
                            }, maxAwaitMilliseconds);
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdOut()).isEmpty();
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final CapturedOutput copyOf = CaptureOutput.of(() -> {
                                writeOutput(System.err, line1, line2);
                            }, maxAwaitMilliseconds);
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdErr()).isEmpty();
                    }
                }
            }

            // copyOf
            public class Copy {

                public class CaptureSystem {

                    @Test
                    public void out() {
                        //when
                        final CapturedOutput captured = CaptureOutput.copyOf(() -> {
                            writeOutput(System.out, line1, line2);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput captured = CaptureOutput.copyOf(() -> {
                            writeOutput(System.err, line1, line2);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(captured.getStdErr()).containsExactly(line1, line2);
                    }

                }

                public class ReplaceSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();
                    private final AtomicReference<PrintStream> replacement = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        CaptureOutput.copyOf(() -> replacement.set(System.out), maxAwaitMilliseconds);
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.copyOf(() -> replacement.set(System.err), maxAwaitMilliseconds);
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }
                }

                public class RestoreSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        CaptureOutput.copyOf(() -> {
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(System.out).isSameAs(original.get());
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.copyOf(() -> {
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(System.err).isSameAs(original.get());
                    }
                }

                public class ExceptionThrown {

                    private final UnsupportedOperationException cause = new UnsupportedOperationException(line1);

                    @Test
                    public void wrappedInOutputCaptureException() {
                        //then
                        assertThatThrownBy(() -> CaptureOutput.copyOf(() -> {
                            throw cause;
                        }, maxAwaitMilliseconds))
                                .isInstanceOf(OutputCaptureException.class)
                                .hasCause(cause);
                    }
                }

                public class FiltersToTargetThread {

                    @Test
                    public void out() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.copyOf(() -> {
                            latchPair.releaseAndWait();
                            System.out.println(line2);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(capturedOutput.getStdOut()).containsExactly(line2);
                    }

                    @Test
                    public void err() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.copyOf(() -> {
                            latchPair.releaseAndWait();
                            System.err.println(line2);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(capturedOutput.getStdErr()).containsExactly(line2);
                    }

                }

                public class CopyToOriginal {

                    private final AtomicReference<CapturedOutput> ref = new AtomicReference<>();

                    @Test
                    public void out() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final CapturedOutput copyOf = CaptureOutput.copyOf(() -> {
                                writeOutput(System.out, line1, line2);
                            }, maxAwaitMilliseconds);
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final CapturedOutput copyOf = CaptureOutput.copyOf(() -> {
                                writeOutput(System.err, line1, line2);
                            }, maxAwaitMilliseconds);
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdErr()).containsExactly(line1, line2);
                    }
                }
            }
        }

        public class AllThreads {

            // ofAll
            public class Redirect {

                public class CaptureSystem {

                    @Test
                    public void out() {
                        //when
                        final CapturedOutput captured = CaptureOutput.ofAll(() -> {
                            writeOutput(System.out, line1, line2);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput captured = CaptureOutput.ofAll(() -> {
                            writeOutput(System.err, line1, line2);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(captured.getStdErr()).containsExactly(line1, line2);
                    }

                }

                public class ReplaceSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();
                    private final AtomicReference<PrintStream> replacement = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        CaptureOutput.ofAll(() -> replacement.set(System.out), maxAwaitMilliseconds);
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.ofAll(() -> replacement.set(System.err), maxAwaitMilliseconds);
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }
                }

                public class RestoreSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        CaptureOutput.ofAll(() -> {
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(System.out).isSameAs(original.get());
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.ofAll(() -> {
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(System.err).isSameAs(original.get());
                    }
                }

                public class ExceptionThrown {

                    private final UnsupportedOperationException cause = new UnsupportedOperationException(line1);

                    @Test
                    public void wrappedInOutputCaptureException() {
                        //then
                        assertThatThrownBy(() -> CaptureOutput.ofAll(() -> {
                            throw cause;
                        }, maxAwaitMilliseconds))
                                .isInstanceOf(OutputCaptureException.class)
                                .hasCause(cause);
                    }
                }

                public class CapturesAllThreads {

                    @Test
                    public void out() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
                        //when
                        final CapturedOutput captured = CaptureOutput.ofAll(() -> {
                            latchPair.releaseAndWait();
                            writeOutput(System.out, line2);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
                        //when
                        final CapturedOutput captured = CaptureOutput.ofAll(() -> {
                            latchPair.releaseAndWait();
                            writeOutput(System.err, line2);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(captured.getStdErr()).containsExactly(line1, line2);
                    }
                }

                public class RedirectFromOriginal {

                    private final AtomicReference<CapturedOutput> ref = new AtomicReference<>();

                    private final AtomicBoolean finished = new AtomicBoolean(false);

                    @Test
                    public void out() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final CapturedOutput copyOf = CaptureOutput.ofAll(() -> {
                                writeOutput(System.out, line1, line2);
                                finished.set(true);
                            }, maxAwaitMilliseconds);
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(finished).isTrue();
                        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdOut()).isEmpty();
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final CapturedOutput copyOf = CaptureOutput.ofAll(() -> {
                                writeOutput(System.err, line1, line2);
                                finished.set(true);
                            }, maxAwaitMilliseconds);
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(finished).isTrue();
                        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdErr()).isEmpty();
                    }
                }
            }
        }
    }

    public class Asynchronous {

        public class ThreadFiltered {

            // ofThread
            public class Redirect {

                public class CaptureSystem {

                    @Test
                    public void out() {
                        //when
                        final OngoingCapturedOutput captured = CaptureOutput.ofThread(() -> {
                            writeOutput(System.out, line1, line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(captured.getCompletedLatch());
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        OngoingCapturedOutput captured = CaptureOutput.ofThread(() -> {
                            writeOutput(System.err, line1, line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(captured.getCompletedLatch());
                        //then
                        assertThat(captured.getStdErr()).containsExactly(line1, line2);
                    }

                }

                public class ReplaceSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();
                    private final AtomicReference<PrintStream> replacement = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        CaptureOutput.ofThread(() -> replacement.set(System.out), maxAwaitMilliseconds);
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.ofThread(() -> replacement.set(System.err), maxAwaitMilliseconds);
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }
                }

                public class RestoreSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        awaitLatch(
                                CaptureOutput.ofThread(() -> {
                                }, maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(System.out).isSameAs(original.get());
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        awaitLatch(
                                CaptureOutput.ofThread(() -> {
                                }, maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(System.err).isSameAs(original.get());
                    }
                }

                public class ExceptionThrown {

                    private final UnsupportedOperationException cause = new UnsupportedOperationException(line1);

                    @Test
                    public void isAvailable() {
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.ofThread(() -> {
                            throw cause;
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        assertThat(capturedOutput.thrownException()).contains(cause);
                    }
                }

                public class FiltersToTargetThread {

                    @Test
                    public void out() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.ofThread(() -> {
                            latchPair.releaseAndWait();
                            System.out.println(line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(capturedOutput.getStdOut()).containsExactly(line2);
                    }

                    @Test
                    public void err() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.ofThread(() -> {
                            latchPair.releaseAndWait();
                            System.err.println(line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(capturedOutput.getStdErr()).containsExactly(line2);
                    }

                }

                public class RedirectFromOriginal {

                    private final AtomicReference<CapturedOutput> ref = new AtomicReference<>();

                    @Test
                    public void out() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final OngoingCapturedOutput copyOf = CaptureOutput.ofThread(() -> {
                                writeOutput(System.out, line1, line2);
                            }, maxAwaitMilliseconds);
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdOut()).isEmpty();
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final OngoingCapturedOutput copyOf = CaptureOutput.ofThread(() -> {
                                writeOutput(System.err, line1, line2);
                            }, maxAwaitMilliseconds);
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdErr()).isEmpty();
                    }
                }

                public class Flush {

                    @Test
                    public void out() {
                        //given
                        final SafeLatch ready = createLatch();
                        final SafeLatch done = createLatch();
                        final OngoingCapturedOutput capturedOutput =
                                CaptureOutput.ofThread(() -> {
                                    System.out.println(line1);
                                    releaseLatch(ready);
                                    awaitLatch(done);
                                    System.out.println(line2);
                                }, maxAwaitMilliseconds);
                        awaitLatch(ready);
                        //when
                        final CapturedOutput initialOutput =
                                capturedOutput.getCapturedOutputAndFlush();
                        releaseLatch(done);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(initialOutput.getStdOut()).containsExactly(line1);
                        assertThat(capturedOutput.getStdOut()).containsExactly(line2);
                        assertThat(initialOutput.out().toString()).isEqualToIgnoringWhitespace(line1);
                        assertThat(capturedOutput.out().toString()).isEqualToIgnoringWhitespace(line2);
                    }

                    @Test
                    public void err() {
                        //given
                        final SafeLatch ready = createLatch();
                        final SafeLatch done = createLatch();
                        final OngoingCapturedOutput capturedOutput =
                                CaptureOutput.ofThread(() -> {
                                    System.err.println(line1);
                                    releaseLatch(ready);
                                    awaitLatch(done);
                                    System.err.println(line2);
                                }, maxAwaitMilliseconds);
                        awaitLatch(ready);
                        //when
                        final CapturedOutput initialOutput =
                                capturedOutput.getCapturedOutputAndFlush();
                        releaseLatch(done);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(initialOutput.getStdErr()).containsExactly(line1);
                        assertThat(capturedOutput.getStdErr()).containsExactly(line2);
                        assertThat(initialOutput.err().toString()).isEqualToIgnoringWhitespace(line1);
                        assertThat(capturedOutput.err().toString()).isEqualToIgnoringWhitespace(line2);
                    }
                }
            }

            // copyOfThread
            public class Copy {

                public class CaptureSystem {

                    @Test
                    public void out() {
                        //when
                        final OngoingCapturedOutput captured = CaptureOutput.copyOfThread(() -> {
                            writeOutput(System.out, line1, line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(captured.getCompletedLatch());
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final OngoingCapturedOutput captured = CaptureOutput.copyOfThread(() -> {
                            writeOutput(System.err, line1, line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(captured.getCompletedLatch());
                        //then
                        assertThat(captured.getStdErr()).containsExactly(line1, line2);
                    }

                }

                public class ReplaceSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();
                    private final AtomicReference<PrintStream> replacement = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        awaitLatch(
                                CaptureOutput.copyOfThread(() -> replacement.set(System.out), maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        awaitLatch(
                                CaptureOutput.copyOfThread(() -> replacement.set(System.err), maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }
                }

                public class RestoreSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        awaitLatch(
                                CaptureOutput.copyOfThread(() -> {
                                }, maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(System.out).isSameAs(original.get());
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        awaitLatch(
                                CaptureOutput.copyOfThread(() -> {
                                }, maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(System.err).isSameAs(original.get());
                    }
                }

                public class ExceptionThrown {

                    private final UnsupportedOperationException cause = new UnsupportedOperationException(line1);

                    @Test
                    public void isAvailable() {
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.copyOfThread(() -> {
                            throw cause;
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(capturedOutput.thrownException()).contains(cause);
                    }
                }

                public class FiltersToTargetThread {

                    @Test
                    public void out() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.copyOfThread(() -> {
                            latchPair.releaseAndWait();
                            System.out.println(line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(capturedOutput.getStdOut()).containsExactly(line2);
                    }

                    @Test
                    public void err() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.copyOfThread(() -> {
                            latchPair.releaseAndWait();
                            System.err.println(line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(capturedOutput.getStdErr()).containsExactly(line2);
                    }

                }

                public class CopyToOriginal {

                    private final AtomicReference<CapturedOutput> ref = new AtomicReference<>();

                    @Test
                    public void out() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final OngoingCapturedOutput copyOf = CaptureOutput.copyOfThread(() -> {
                                writeOutput(System.out, line1, line2);
                            }, maxAwaitMilliseconds);
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final OngoingCapturedOutput copyOf = CaptureOutput.copyOfThread(() -> {
                                writeOutput(System.err, line1, line2);
                            }, maxAwaitMilliseconds);
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdErr()).containsExactly(line1, line2);
                    }
                }

                public class Flush {

                    @Test
                    public void out() {
                        //given
                        final SafeLatch ready = createLatch();
                        final SafeLatch done = createLatch();
                        final OngoingCapturedOutput capturedOutput =
                                CaptureOutput.copyOfThread(() -> {
                                    System.out.println(line1);
                                    releaseLatch(ready);
                                    awaitLatch(done);
                                    System.out.println(line2);
                                }, maxAwaitMilliseconds);
                        awaitLatch(ready);
                        //when
                        final CapturedOutput initialOutput =
                                capturedOutput.getCapturedOutputAndFlush();
                        releaseLatch(done);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(initialOutput.getStdOut()).containsExactly(line1);
                        assertThat(capturedOutput.getStdOut()).containsExactly(line2);
                        assertThat(initialOutput.out().toString()).isEqualToIgnoringWhitespace(line1);
                        assertThat(capturedOutput.out().toString()).isEqualToIgnoringWhitespace(line2);
                    }

                    @Test
                    public void err() {
                        //given
                        final SafeLatch ready = createLatch();
                        final SafeLatch done = createLatch();
                        final OngoingCapturedOutput capturedOutput =
                                CaptureOutput.copyOfThread(() -> {
                                    System.err.println(line1);
                                    releaseLatch(ready);
                                    awaitLatch(done);
                                    System.err.println(line2);
                                }, maxAwaitMilliseconds);
                        awaitLatch(ready);
                        //when
                        final CapturedOutput initialOutput =
                                capturedOutput.getCapturedOutputAndFlush();
                        releaseLatch(done);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(initialOutput.getStdErr()).containsExactly(line1);
                        assertThat(capturedOutput.getStdErr()).containsExactly(line2);
                        assertThat(initialOutput.err().toString()).isEqualToIgnoringWhitespace(line1);
                        assertThat(capturedOutput.err().toString()).isEqualToIgnoringWhitespace(line2);
                    }
                }
            }
        }

        public class AllThreads {

            // whileDoing
            public class Redirect {

                public class CaptureSystem {

                    @Test
                    public void out() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
                        //when
                        final OngoingCapturedOutput captured = CaptureOutput.whileDoing(() -> {
                            latchPair.releaseAndWait();
                            writeOutput(System.out, line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(captured.getCompletedLatch());
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
                        //when
                        OngoingCapturedOutput captured = CaptureOutput.whileDoing(() -> {
                            latchPair.releaseAndWait();
                            writeOutput(System.err, line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(captured.getCompletedLatch());
                        //then
                        assertThat(captured.getStdErr()).containsExactly(line1, line2);
                    }

                }

                public class ReplaceSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();
                    private final AtomicReference<PrintStream> replacement = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        CaptureOutput.whileDoing(() -> replacement.set(System.out), maxAwaitMilliseconds);
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.whileDoing(() -> replacement.set(System.err), maxAwaitMilliseconds);
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }
                }

                public class RestoreSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        awaitLatch(
                                CaptureOutput.whileDoing(() -> {
                                }, maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(System.out).isSameAs(original.get());
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        awaitLatch(
                                CaptureOutput.whileDoing(() -> {
                                }, maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(System.err).isSameAs(original.get());
                    }
                }

                public class ExceptionThrown {

                    private final UnsupportedOperationException cause = new UnsupportedOperationException(line1);

                    @Test
                    public void isAvailable() {
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.whileDoing(() -> {
                            throw cause;
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        assertThat(capturedOutput.thrownException()).contains(cause);
                    }
                }

                public class CapturesAllThreads {

                    @Test
                    public void out() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.whileDoing(() -> {
                            latchPair.releaseAndWait();
                            System.out.println(line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(capturedOutput.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.whileDoing(() -> {
                            latchPair.releaseAndWait();
                            System.err.println(line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(capturedOutput.getStdErr()).containsExactly(line1, line2);
                    }

                }

                public class RedirectFromOriginal {

                    private final AtomicReference<CapturedOutput> ref = new AtomicReference<>();

                    @Test
                    public void out() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final OngoingCapturedOutput copyOf = CaptureOutput.whileDoing(() -> {
                                writeOutput(System.out, line1, line2);
                            }, maxAwaitMilliseconds);
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdOut()).isEmpty();
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final OngoingCapturedOutput copyOf = CaptureOutput.whileDoing(() -> {
                                writeOutput(System.err, line1, line2);
                            }, maxAwaitMilliseconds);
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdErr()).isEmpty();
                    }
                }

                public class Flush {

                    @Test
                    public void out() {
                        //given
                        final SafeLatch ready = createLatch();
                        final SafeLatch done = createLatch();
                        final OngoingCapturedOutput capturedOutput =
                                CaptureOutput.whileDoing(() -> {
                                    releaseLatch(ready);
                                    awaitLatch(done);
                                }, maxAwaitMilliseconds);
                        awaitLatch(ready);
                        //when
                        System.out.println(line1);
                        final CapturedOutput initialOutput =
                                capturedOutput.getCapturedOutputAndFlush();
                        System.out.println(line2);
                        releaseLatch(done);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(initialOutput.getStdOut()).containsExactly(line1);
                        assertThat(capturedOutput.getStdOut()).containsExactly(line2);
                        assertThat(initialOutput.out().toString()).isEqualToIgnoringWhitespace(line1);
                        assertThat(capturedOutput.out().toString()).isEqualToIgnoringWhitespace(line2);
                    }

                    @Test
                    public void err() {
                        //given
                        final SafeLatch ready = createLatch();
                        final SafeLatch done = createLatch();
                        final OngoingCapturedOutput capturedOutput =
                                CaptureOutput.whileDoing(() -> {
                                    releaseLatch(ready);
                                    awaitLatch(done);
                                }, maxAwaitMilliseconds);
                        awaitLatch(ready);
                        //when
                        System.err.println(line1);
                        final CapturedOutput initialOutput =
                                capturedOutput.getCapturedOutputAndFlush();
                        System.err.println(line2);
                        releaseLatch(done);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(initialOutput.getStdErr()).containsExactly(line1);
                        assertThat(capturedOutput.getStdErr()).containsExactly(line2);
                        assertThat(initialOutput.err().toString()).isEqualToIgnoringWhitespace(line1);
                        assertThat(capturedOutput.err().toString()).isEqualToIgnoringWhitespace(line2);
                    }
                }
            }

            // copyWhileDoing
            public class Copy {

                public class CaptureSystem {

                    @Test
                    public void out() {
                        //when
                        final OngoingCapturedOutput captured = CaptureOutput.copyWhileDoing(() -> {
                            writeOutput(System.out, line1, line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(captured.getCompletedLatch());
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final OngoingCapturedOutput captured = CaptureOutput.copyWhileDoing(() -> {
                            writeOutput(System.err, line1, line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(captured.getCompletedLatch());
                        //then
                        assertThat(captured.getStdErr()).containsExactly(line1, line2);
                    }

                }

                public class ReplaceSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();
                    private final AtomicReference<PrintStream> replacement = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        awaitLatch(
                                CaptureOutput.copyWhileDoing(() -> replacement.set(System.out), maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        awaitLatch(
                                CaptureOutput.copyWhileDoing(() -> replacement.set(System.err), maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }
                }

                public class RestoreSystem {

                    private final AtomicReference<PrintStream> original = new AtomicReference<>();

                    @Test
                    public void out() {
                        //given
                        original.set(System.out);
                        //when
                        awaitLatch(
                                CaptureOutput.copyWhileDoing(() -> {
                                }, maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(System.out).isSameAs(original.get());
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        awaitLatch(
                                CaptureOutput.copyWhileDoing(() -> {
                                }, maxAwaitMilliseconds)
                                        .getCompletedLatch());
                        //then
                        assertThat(System.err).isSameAs(original.get());
                    }
                }

                public class ExceptionThrown {

                    private final UnsupportedOperationException cause = new UnsupportedOperationException(line1);

                    @Test
                    public void isAvailable() {
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.copyWhileDoing(() -> {
                            throw cause;
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(capturedOutput.thrownException()).contains(cause);
                    }
                }

                public class CapturesAllThreads {

                    @Test
                    public void out() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.copyWhileDoing(() -> {
                            latchPair.releaseAndWait();
                            System.out.println(line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(capturedOutput.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.err.println(line1));
                        //when
                        final OngoingCapturedOutput capturedOutput = CaptureOutput.copyWhileDoing(() -> {
                            latchPair.releaseAndWait();
                            System.err.println(line2);
                        }, maxAwaitMilliseconds);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(capturedOutput.getStdErr()).containsExactly(line1, line2);
                    }

                }

                public class CopyToOriginal {

                    private final AtomicReference<CapturedOutput> ref = new AtomicReference<>();

                    @Test
                    public void out() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final OngoingCapturedOutput copyOf = CaptureOutput.copyWhileDoing(() -> {
                                writeOutput(System.out, line1, line2);
                            }, maxAwaitMilliseconds);
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdOut()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput capturedOutput = CaptureOutput.ofAll(() -> {
                            final OngoingCapturedOutput copyOf = CaptureOutput.copyWhileDoing(() -> {
                                writeOutput(System.err, line1, line2);
                            }, maxAwaitMilliseconds);
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        }, maxAwaitMilliseconds);
                        //then
                        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdErr()).containsExactly(line1, line2);
                    }
                }

                public class Flush {

                    @Test
                    public void out() {
                        //given
                        final SafeLatch ready = createLatch();
                        final SafeLatch done = createLatch();
                        final OngoingCapturedOutput capturedOutput =
                                CaptureOutput.copyWhileDoing(() -> {
                                    releaseLatch(ready);
                                    awaitLatch(done);
                                }, maxAwaitMilliseconds);
                        awaitLatch(ready);
                        //when
                        System.out.println(line1);
                        final CapturedOutput initialOutput =
                                capturedOutput.getCapturedOutputAndFlush();
                        System.out.println(line2);
                        releaseLatch(done);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(initialOutput.getStdOut()).containsExactly(line1);
                        assertThat(capturedOutput.getStdOut()).containsExactly(line2);
                        assertThat(initialOutput.out().toString()).isEqualToIgnoringWhitespace(line1);
                        assertThat(capturedOutput.out().toString()).isEqualToIgnoringWhitespace(line2);
                    }

                    @Test
                    public void err() {
                        //given
                        final SafeLatch ready = createLatch();
                        final SafeLatch done = createLatch();
                        final OngoingCapturedOutput capturedOutput =
                                CaptureOutput.copyWhileDoing(() -> {
                                    releaseLatch(ready);
                                    awaitLatch(done);
                                }, maxAwaitMilliseconds);
                        awaitLatch(ready);
                        //when
                        System.err.println(line1);
                        final CapturedOutput initialOutput =
                                capturedOutput.getCapturedOutputAndFlush();
                        System.err.println(line2);
                        releaseLatch(done);
                        awaitLatch(capturedOutput.getCompletedLatch());
                        //then
                        assertThat(initialOutput.getStdErr()).containsExactly(line1);
                        assertThat(capturedOutput.getStdErr()).containsExactly(line2);
                        assertThat(initialOutput.err().toString()).isEqualToIgnoringWhitespace(line1);
                        assertThat(capturedOutput.err().toString()).isEqualToIgnoringWhitespace(line2);
                    }
                }
            }
        }
    }

    private LatchPair whenReleased(final Runnable callable) {
        final LatchPair latchPair = new LatchPair();
        new Thread(() -> {
            awaitLatch(latchPair.ready);
            callable.run();
            releaseLatch(latchPair.done);
        }).start();
        return latchPair;
    }

    private void writeOutput(final PrintStream out, final String... lines) {
        for (String line : lines) {
            out.println(line);
        }
    }

    private void awaitLatch(final SafeLatch latch) {
            latch.await();
    }

    @Test
    public void canRestoreSystemOutAndErrWhenMultipleOutputCapturesOverlap() throws InterruptedException {
        //given
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final SafeLatch latch1 = createLatch();
        final SafeLatch latch2 = createLatch();
        final SafeLatch latch3 = createLatch();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            awaitLatch(latch1);
            CaptureOutput.of(waitAndContinue(latch2, latch3), maxAwaitMilliseconds);
        });
        executor.submit(executor::shutdown);
        //when
        CaptureOutput.of(waitAndContinue(latch1, latch2), maxAwaitMilliseconds);
        releaseLatch(latch3);
        executor.awaitTermination(100L, TimeUnit.MILLISECONDS);
        //then
        assertThat(System.out).isSameAs(originalOut);
        assertThat(System.err).isSameAs(originalErr);
    }

    private void releaseLatch(final SafeLatch latch) {
        latch.countDown();
    }

    private SafeLatch createLatch() {
        return new SafeLatch(1, 1000L);
    }

    private void asyncWithInterrupt(final SafeLatch ready, final SafeLatch done) {
        System.out.println(STARTING_OUT);
        System.err.println(STARTING_ERR);
        releaseLatch(ready);
        awaitLatch(done);
        System.out.println(FINISHED_OUT);
        System.err.println(FINISHED_ERR);
    }

    private ThrowingCallable waitAndContinue(final SafeLatch ready, final SafeLatch done) {
        return () -> {
            releaseLatch(ready);
            awaitLatch(done);
        };
    }

    private class LatchPair {
        private final SafeLatch ready = new SafeLatch(1, 1000L);
        private final SafeLatch done = new SafeLatch(1, 1000L);

        void releaseAndWait() {
            ready.countDown();
            awaitLatch(done);
        }
    }
}
