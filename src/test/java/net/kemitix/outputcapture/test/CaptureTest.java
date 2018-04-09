package net.kemitix.outputcapture.test;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import lombok.val;
import net.kemitix.conditional.Action;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(HierarchicalContextRunner.class)
public class CaptureTest {

    //<editor-fold desc="fields">
    private static final long A_PERIOD = 200L;

    private static final long A_SHORT_PERIOD = 100L;
    private static final String STARTING_OUT = "starting out";
    private static final String STARTING_ERR = "starting err";
    private static final String FINISHED_OUT = "finished out";
    private static final String FINISHED_ERR = "finished err";

    private String line1 = "1:" + UUID.randomUUID().toString();

    private String line2 = "2:" + UUID.randomUUID().toString();
    //</editor-fold>

    @Rule
    public Timeout globalTimeout = Timeout.seconds(A_SHORT_PERIOD);

    @Before
    public void setUp() {
        assertThat(CaptureOutput.activeCount()).as("No existing captures").isZero();
    }

    @After
    public void tearDown() {
        assertThat(CaptureOutput.activeCount()).as("All captures removed").isZero();
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
                        });
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput captured = CaptureOutput.of(() -> {
                            writeOutput(System.err, line1, line2);
                        });
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
                        CaptureOutput.of(() -> replacement.set(System.out));
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.of(() -> replacement.set(System.err));
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
                        });
                        //then
                        assertThat(System.out).isSameAs(original.get());
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.of(() -> {
                        });
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
                        }))
                                .isInstanceOf(OutputCaptureException.class)
                                .hasCause(cause);
                    }
                }

                public class FiltersToTargetThread {

                    private final ExecutorService catchMe = Executors.newSingleThreadExecutor();
                    private final ExecutorService ignoreMe = Executors.newSingleThreadExecutor();
                    private final AtomicReference<CapturedOutput> reference = new AtomicReference<>();
                    private final CountDownLatch ready = createLatch();
                    private final CountDownLatch done = createLatch();
                    private final CountDownLatch finished = createLatch();

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
                            final CountDownLatch ready,
                            final CountDownLatch done,
                            final AtomicReference<CapturedOutput> reference,
                            final CountDownLatch finished,
                            final ExecutorService catchMe
                    ) {
                        final CapturedOutput capturedOutput =
                                CaptureOutput.of(() -> asyncWithInterrupt(ready, done));
                        reference.set(capturedOutput);
                        releaseLatch(finished);
                        catchMe.shutdown();
                    }

                    private void ignoreThis(
                            final CountDownLatch ready,
                            final CountDownLatch done,
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
                            });
                            ref.set(copyOf);
                        });
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
                            });
                            ref.set(copyOf);
                        });
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
                        });
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput captured = CaptureOutput.copyOf(() -> {
                            writeOutput(System.err, line1, line2);
                        });
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
                        CaptureOutput.copyOf(() -> replacement.set(System.out));
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.copyOf(() -> replacement.set(System.err));
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
                        });
                        //then
                        assertThat(System.out).isSameAs(original.get());
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.copyOf(() -> {
                        });
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
                        }))
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
                        });
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
                        });
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
                            });
                            ref.set(copyOf);
                        });
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
                            });
                            ref.set(copyOf);
                        });
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
                        });
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final CapturedOutput captured = CaptureOutput.ofAll(() -> {
                            writeOutput(System.err, line1, line2);
                        });
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
                        CaptureOutput.ofAll(() -> replacement.set(System.out));
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.ofAll(() -> replacement.set(System.err));
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
                        });
                        //then
                        assertThat(System.out).isSameAs(original.get());
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.ofAll(() -> {
                        });
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
                        }))
                                .isInstanceOf(OutputCaptureException.class)
                                .hasCause(cause);
                    }
                }

                public class CapturesAllThread {

                    @Test
                    public void out() {
                        //given
                        final LatchPair latchPair = whenReleased(() -> System.out.println(line1));
                        //when
                        final CapturedOutput captured = CaptureOutput.ofAll(() -> {
                            latchPair.releaseAndWait();
                            writeOutput(System.out, line2);
                        });
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
                        });
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
                            });
                            ref.set(copyOf);
                        });
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
                            });
                            ref.set(copyOf);
                        });
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
                        });
                        awaitLatch(captured.getCompletedLatch());
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        OngoingCapturedOutput captured = CaptureOutput.ofThread(() -> {
                            writeOutput(System.err, line1, line2);
                        });
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
                        CaptureOutput.ofThread(() -> replacement.set(System.out));
                        //then
                        assertThat(replacement).isNotSameAs(original);
                    }

                    @Test
                    public void err() {
                        //given
                        original.set(System.err);
                        //when
                        CaptureOutput.ofThread(() -> replacement.set(System.err));
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
                                })
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
                                })
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
                        });
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
                        });
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
                        });
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
                            });
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        });
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
                            });
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        });
                        //then
                        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdErr()).isEmpty();
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
                        });
                        awaitLatch(captured.getCompletedLatch());
                        //then
                        assertThat(captured.getStdOut()).containsExactly(line1, line2);
                    }

                    @Test
                    public void err() {
                        //when
                        final OngoingCapturedOutput captured = CaptureOutput.copyOfThread(() -> {
                            writeOutput(System.err, line1, line2);
                        });
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
                                CaptureOutput.copyOfThread(() -> replacement.set(System.out))
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
                                CaptureOutput.copyOfThread(() -> replacement.set(System.err))
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
                                })
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
                                })
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
                        });
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
                        });
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
                        });
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
                            });
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        });
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
                            });
                            awaitLatch(copyOf.getCompletedLatch());
                            ref.set(copyOf);
                        });
                        //then
                        assertThat(ref.get().getStdErr()).containsExactly(line1, line2);
                        assertThat(capturedOutput.getStdErr()).containsExactly(line1, line2);
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

    private void awaitLatch(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new OutputCaptureException(e);
        }
    }

    @Test
    public void canRestoreSystemOutAndErrWhenMultipleOutputCapturesOverlap() throws InterruptedException {
        //given
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final CountDownLatch latch1 = createLatch();
        final CountDownLatch latch2 = createLatch();
        final CountDownLatch latch3 = createLatch();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            awaitLatch(latch1);
            CaptureOutput.of(waitAndContinue(latch2, latch3));
        });
        executor.submit(executor::shutdown);
        //when
        CaptureOutput.of(waitAndContinue(latch1, latch2));
        releaseLatch(latch3);
        executor.awaitTermination(A_SHORT_PERIOD, TimeUnit.MILLISECONDS);
        //then
        assertThat(System.out).isSameAs(originalOut);
        assertThat(System.err).isSameAs(originalErr);
    }

    private void releaseLatch(final CountDownLatch latch) {
        latch.countDown();
    }

    private CountDownLatch createLatch() {
        return new CountDownLatch(1);
    }

    @Test
    //Asynchronous.ThreadFiltered.Redirect
    public void canCaptureOutputAsynchronously() {
        //given
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final CountDownLatch latch1 = createLatch();
        final CountDownLatch latch2 = createLatch();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.ofThread(() -> {
            asyncWithInterrupt(latch1, latch2);
        });
        awaitLatch(latch1);
        final Stream<String> stdOut = ongoingCapturedOutput.getStdOut();
        final Stream<String> stdErr = ongoingCapturedOutput.getStdErr();
        releaseLatch(latch2);
        awaitLatch(ongoingCapturedOutput.getCompletedLatch());
        //then
        assertThat(stdOut).containsExactly(STARTING_OUT);
        assertThat(stdErr).containsExactly(STARTING_ERR);
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(STARTING_OUT, FINISHED_OUT);
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly(STARTING_ERR, FINISHED_ERR);
        awaitLatch(ongoingCapturedOutput.getCompletedLatch());
        assertThat(System.out).as("restore original out").isSameAs(originalOut);
        assertThat(System.err).as("restore original err").isSameAs(originalErr);
    }

    private void asyncWithInterrupt(final CountDownLatch ready, final CountDownLatch done) {
        System.out.println(STARTING_OUT);
        System.err.println(STARTING_ERR);
        releaseLatch(ready);
        awaitLatch(done);
        System.out.println(FINISHED_OUT);
        System.err.println(FINISHED_ERR);
    }

    @Test
    //Asynchronous.ThreadFiltered.Redirect
    public void canRestoreNormalSystemOutWhenCapturingAsynchronously() {
        //when
        final CapturedOutput outerCaptured = CaptureOutput.of(() -> {
            final OngoingCapturedOutput innerCaptured = CaptureOutput.ofThread(() -> {
                System.out.println(line1);
            });
            innerCaptured.await(A_PERIOD, TimeUnit.MILLISECONDS);
            System.out.println(line2);
            assertThat(innerCaptured.getStdOut()).containsExactly(line1)
                    .doesNotContain(line2);
        });
        //then
        assertThat(outerCaptured.getStdOut())
                .containsExactly(line2)
                .doesNotContain(line1);
    }

    @Test
    //Asynchronous.ThreadFiltered.Redirect
    public void canRestoreNormalSystemErrWhenCapturingAsynchronously() {
        //when
        final CapturedOutput outerCaptured = CaptureOutput.of(() -> {
            final OngoingCapturedOutput innerCaptured = CaptureOutput.ofThread(() -> {
                System.err.println(line1);
            });
            innerCaptured.await(A_PERIOD, TimeUnit.MILLISECONDS);
            System.err.println(line2);
            assertThat(innerCaptured.getStdErr()).containsExactly(line1)
                    .doesNotContain(line2);
        });
        //then
        assertThat(outerCaptured.getStdErr())
                .containsExactly(line2)
                .doesNotContain(line1);
    }

    @Test
    //Asynchronous.*.Copy
    public void canFlushCapturedOutputWhenCapturingAsynchronously() {
        //given
        final CountDownLatch readyToFlush = createLatch();
        final CountDownLatch flushCompleted = createLatch();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.copyOfThread(() -> {
            asyncWithInterrupt(readyToFlush, flushCompleted);
        });
        awaitLatch(readyToFlush);
        final Stream<String> stdOut = ongoingCapturedOutput.getStdOut();
        final Stream<String> stdErr = ongoingCapturedOutput.getStdErr();
        ongoingCapturedOutput.flush();
        releaseLatch(flushCompleted);
        awaitLatch(ongoingCapturedOutput.getCompletedLatch());
        //then
        assertThat(stdOut).containsExactly(STARTING_OUT);
        assertThat(stdErr).containsExactly(STARTING_ERR);
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(FINISHED_OUT);
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly(FINISHED_ERR);
    }

    @Test
    //Asynchronous.*.Redirect
    public void canCapturedOutputAndFlushWhenCapturingAsynchronously() {
        //given
        val readyToFlush = createLatch();
        val flushCompleted = createLatch();
        //when
        val ongoingCapturedOutput = CaptureOutput.ofThread(() -> {
            asyncWithInterrupt(readyToFlush, flushCompleted);
        });
        awaitLatch(readyToFlush);
        val initialCapturedOutput = ongoingCapturedOutput.getCapturedOutputAndFlush();
        releaseLatch(flushCompleted);
        //then
        try {
            assertThat(initialCapturedOutput.getStdOut()).containsExactly(STARTING_OUT);
            assertThat(initialCapturedOutput.getStdErr()).containsExactly(STARTING_ERR);
        } finally {
            awaitLatch(ongoingCapturedOutput.getCompletedLatch());
        }
        assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(FINISHED_OUT);
        assertThat(ongoingCapturedOutput.getStdErr()).containsExactly(FINISHED_ERR);
    }

    @Test
    //Asynchronous.*.*
    public void canWaitForThreadToComplete() {
        //given
        final CountDownLatch latch = createLatch();
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.ofThread(() -> {
            awaitLatch(latch);
        });
        //then
        assertThat(ongoingCapturedOutput.isRunning()).as("isRunning = true")
                .isTrue();
        assertThat(ongoingCapturedOutput.isShutdown()).as("isShutdown = false")
                .isFalse();
        releaseLatch(latch);
        ongoingCapturedOutput.await(A_SHORT_PERIOD, TimeUnit.MILLISECONDS);
        assertThat(ongoingCapturedOutput.isRunning()).as("isRunning = false")
                .isFalse();
        assertThat(ongoingCapturedOutput.isShutdown()).as("isShutdown = true")
                .isTrue();
    }

    @Test
    //Asynchronous.*.*
    public void exceptionThrownInThreadIsAvailableToOngoingCapturedOutput() {
        //given
        final OutputCaptureException outputCaptureException = new OutputCaptureException("");
        //when
        final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.ofThread(() -> {
            throw outputCaptureException;
        });
        //then
        ongoingCapturedOutput.await(A_PERIOD, TimeUnit.MILLISECONDS);
        assertThat(ongoingCapturedOutput.thrownException()).contains(outputCaptureException);
    }

    @Test
    //Asynchronous.ThreadFiltered.Copy
    public void canCaptureCopyOfThread() {
        //when
        val capturedOutput = CaptureOutput.copyOfThread(() -> {
            System.out.println(line1);
        });
        //then
        awaitLatch(capturedOutput.getCompletedLatch());
        assertThat(capturedOutput.isShutdown()).isTrue();
        assertThat(capturedOutput.getStdOut()).containsExactly(line1);
    }

    @Test
    //Asynchronous.All.Copy
    public void canCaptureCopyWhileDoing() {
        //given
        val reference = new AtomicReference<CapturedOutput>();
        val action = new ThrowingCallable() {
            @Override
            public void call() {
                val ready = createLatch();
                val done = createLatch();
                val capturedOutput = CaptureOutput.copyWhileDoing(waitAndContinue(ready, done));
                doBetween(ready, done, () -> System.out.println(line1));
                awaitLatch(capturedOutput.getCompletedLatch());
                reference.set(capturedOutput);
            }
        };
        //when
        final CapturedOutput outer = CaptureOutput.of(action);
        //then
        assertThat(reference.get().getStdOut()).as("capture output").containsExactly(line1);
        assertThat(outer.getStdOut()).as("copy to original").containsExactly(line1);
    }

    @Test
    //Asynchronous.All.Redirect
    public void canCaptureWhileDoing() {
        //given
        val reference = new AtomicReference<CapturedOutput>();
        val action = new ThrowingCallable() {
            @Override
            public void call() {
                val ready = createLatch();
                val done = createLatch();
                val capturedOutput = CaptureOutput.whileDoing(waitAndContinue(ready, done));
                doBetween(ready, done, () -> System.out.println(line1));
                awaitLatch(capturedOutput.getCompletedLatch());
                reference.set(capturedOutput);
            }
        };
        //when
        final CapturedOutput outer = CaptureOutput.of(action);
        //then
        assertThat(reference.get().getStdOut()).containsExactly(line1);
        assertThat(outer.getStdOut()).isEmpty();
    }

    private ThrowingCallable waitAndContinue(final CountDownLatch ready, final CountDownLatch done) {
        return () -> {
            releaseLatch(ready);
            awaitLatch(done);
        };
    }

    private void doBetween(final CountDownLatch ready, final CountDownLatch done, final Action action) {
        awaitLatch(ready);
        action.perform();
        releaseLatch(done);
    }

    @Test
    //Asynchronous.*.Copy
    public void canCaptureOutputAndCopyItToNormalOutputsWhenCapturingAsynchronously() {
        //when
        val outerCaptured = CaptureOutput.copyWhileDoing(() -> {
            assertThat(CaptureOutput.activeCount()).isEqualTo(1);
            val innerCaptured = CaptureOutput.copyOfThread(() -> {
                assertThat(CaptureOutput.activeCount()).isEqualTo(2);
                System.out.println(line1);
            });
            innerCaptured.await(A_PERIOD, TimeUnit.MILLISECONDS);
            awaitLatch(innerCaptured.getCompletedLatch());
            assertThat(CaptureOutput.activeCount()).isEqualTo(1);
            System.out.println(line2);
            assertThat(innerCaptured.getStdOut()).as("inner")
                    .containsExactly(line1, line2)
                    .doesNotContain(line2);
        });
        //then
        assertThat(outerCaptured).isNotNull();
        awaitLatch(outerCaptured.getCompletedLatch());
        assertThat(outerCaptured.getStdOut()).as("outer").containsExactly(line1, line2);
    }

    private class LatchPair {
        private final CountDownLatch ready = new CountDownLatch(1);
        private final CountDownLatch done = new CountDownLatch(1);

        void releaseAndWait() {
            ready.countDown();
            awaitLatch(done);
        }
    }
}
