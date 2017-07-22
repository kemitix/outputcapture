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
singleton's within the JVM, the capturing is not thread-safe. Another
thread may have it's output captured. This may, or may not, be your
intended purpose.
