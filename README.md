# OUTPUT-CAPTURE

Capture output written to `System.out` and `System.err`.

## Usage

```java
    CaptureOutput captureOutput = new CaptureOutput();
    Runnable runnable = () -> {
                                System.out.println(line1);
                                System.out.println(line2);
                                System.err.println(line3);
                            };
    CapturedOutput capturedOutput = captureOutput.of(runnable);
    assertThat(capturedOutput.getStdOut()).containsExactly(line1, line2);
    assertThat(capturedOutput.getStdErr()).containsExactly(line3);
```

## Important

Because the `System.out` and `System.err` are implemented as
singleton's within the JVM, the capturing is not thread-safe. If two
instances of `CaptureOutput` are in effect at the same time and are
not strictly nested (i.e. A starts, B starts, B finishes, A finishes)
then `System.out` and `System.err` will not be restored properly once
capturing is finished.
