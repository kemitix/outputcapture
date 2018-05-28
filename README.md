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
* `OngoingCapturedOutput copyOfThread(ThrowingCallable)`

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

### Stream API

CapturedOutput provides a `stream()` method which returns `Stream<CapturedOutputLine>`. e.g.

#### Synchronous

```java
final List<String> stdOut =
        CaptureOutput.of(() -> {
                        System.out.println(line1Out);
                        System.err.println(line1Err);
                        System.out.println(line2Out);
                        System.err.println(line2Err);
        })
                .stream()
                .filter(CapturedOutputLine::isOut)
                .map(CapturedOutputLine::asString)
                .collect(Collectors.toList());
assertThat(stdOut).containsExactly(line1Out, line2Out);
```

#### Asynchronous

```java
final List<String> stdErr =
        CaptureOutput.ofThread(() -> {
                       System.out.println(line1Out);
                       System.err.println(line1Err);
                       System.out.println(line2Out);
                       System.err.println(line2Err);
        }, timeoutMilliseconds)
                .stream()
                .filter(CapturedOutputLine::isErr)
                .map(CapturedOutputLine::asString)
                .collect(Collectors.toList());
assertThat(stdErr).containsExactly(line1Err, line2Err);
```

With asyncronous, the `stream()` method will bock until the thread completes, or the timeout elapses before returning.

## Important

Output is only captured if it on the main thread the submitted
`ThrowningCallable` is running on. If a new thread is created within the
callable, then any output will not be captured from that thread.
