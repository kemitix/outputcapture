# OUTPUT-CAPTURE

Capture output written to `System.out` and `System.err`.

## Usage

### Recommended

Using try-with-resources:
```java
    try (OutputCapture capture = OutputCapture.begin()) {
        System.out.println(line1);
        System.out.println(line2);
        assertThat(capture.getStdOut()).containsExactly(line1, line2);
    }
```

Always use try-with-resources, or use a `try-finally` with `capture.close()` in the finally block. If you don't then output will not be released and may result in an out-of-memory condition.

## Important

Because the `System.out` and `System.err` are implemented as
singleton's within the JVM, the capturing is not thread-safe. Another
thread may have it's output captured. This may, or may not, be your
intended purpose.
