# OUTPUT-CAPTURE

Capture output written to `System.out` and `System.err`.

## Usage

### Synchronous

* `CaptureOutput.of(ThrowingCallable)`
* `CaptureOutput.copyOf(ThrowingCallable)`
* `CaptureOutput.ofAll(ThrowingCallable)`

With this usage, the callable is executed and completes before the
`capturedOutput` is returned.

The `of` variant prevents output withing the callable from being written
to the original `System.out` and `System.err`, while `copyOf` does not.

```java
ThrowingCallable callable = () -> {
                                   System.out.println(line1);
                                   System.out.println(line2);
                                   System.err.println(line3);
                                  };
CapturedOutput capturedOutput = CaptureOutput.of(callable);
assertThat(capturedOutput.getStdOut()).containsExactly(line1, line2);
assertThat(capturedOutput.getStdErr()).containsExactly(line3);
```

### Asynchronous

* `OngoingCapturedOutput.ofThread(ThrowingCallable, maxAwaitMilliseconds)`
* `OngoingCapturedOutput.copyOfThread(ThrowingCallable, maxAwaitMilliseconds)`
* `OngoingCapturedOutput.whileDoing(ThrowingCallable, maxAwaitMilliseconds)`
* `OngoingCapturedOutput.copyWhileDoing(ThrowingCallable, maxAwaitMilliseconds)`

With this usage, the callable is started and an`ongoingCapturedOutput` is
returned immediately.

```java
//given
final String line1 = "line 1";
final String line2 = "line 2";
ThrowingCallable runnable = () -> {
    System.out.println(line1);
    System.out.println(line2);
};
//when
final OngoingCapturedOutput ongoingCapturedOutput = CaptureOutput.ofThread(runnable, 100L);
//then
// do other things
ongoingCapturedOutput.join();
assertThat(ongoingCapturedOutput.getStdOut()).containsExactly(line1, line2);
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
