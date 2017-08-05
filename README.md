# OUTPUT-CAPTURE

Capture output written to `System.out` and `System.err`.

## Usage

### Synchronous

* `CapturedOutput of(ThrowingCallable)`
* `CapturedOutput copyOf(ThrowingCallable)`

With this usage, the `runnable` is executed and completes before the
`capturedOutput` is returned.

The `of` variant prevents output withing the callable from being written
to the original `System.out` and `System.err`, while `copyOf` does not.

```java
CaptureOutput captureOutput = new CaptureOutput();
ThrowingCallable callable = () -> {
                                   System.out.println(line1);
                                   System.out.println(line2);
                                   System.err.println(line3);
                                  };
CapturedOutput capturedOutput = captureOutput.of(runnable);
assertThat(capturedOutput.getStdOut()).containsExactly(line1, line2);
assertThat(capturedOutput.getStdErr()).containsExactly(line3);
```

### Asynchronous

* `OngoingCapturedOutput ofThread(ThrowingCallable)`

With this usage, the `runnable` is started and the `ongoingCapturedOutput` is
returned immediately.

```java
CaptureOutput captureOutput = new CaptureOutput();
ThrowingCallable runnable = () -> {
                                   System.out.println(line1);

                                   //time passes

                                   System.out.println(line2);

                                   //more time passes

                                   System.out.println(line3);

                                   //still more time passes

                                   System.out.println(line4);
                                  };
OngoingCapturedOutput ongoingCapturedOutput = captureOutput.ofThread(runnable);

assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(line1);

//time passes

assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(line1, line2);

ongoingCapturedOutput.flush();

//more time passes

assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(line3);

CapturedOutput capturedOutput = ongoingCapturedOutput.getCapturedOutputAndFlush();
assertThat(capturedOutput.getStdOut()).containsExactly(line3);

//still more time passes

assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(line4);
assertThat(capturedOutput.getStdOut()).containsExactly(line3);

ongoingCapturedOutput.await(1000L, TimeUnit.MILLISECONDS);
assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(line4);
assertThat(capturedOutput.getStdOut()).containsExactly(line3);
```

## Important

Because the `System.out` and `System.err` are implemented as
singleton's within the JVM, the capturing is not thread-safe. If two
instances of `CaptureOutput` are in effect at the same time and are
not strictly nested (i.e. A starts, B starts, B finishes, A finishes)
then `System.out` and `System.err` will not be restored properly once
capturing is finished and a `OutputCaptureException` will be thrown.
