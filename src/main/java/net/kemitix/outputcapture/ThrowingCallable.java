/*
  The MIT License (MIT)

  Copyright (c) 2018 Paul Campbell

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the "Software"), to deal in the Software without restriction,
  including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
  and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies
  or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
  AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.kemitix.outputcapture;

/**
 * A task that does not return any result and may throw an exception.
 *
 * <p>Implementors define a single method with no arguments called call. The ThrowingCallable interface
 * is similar to Runnable and Callable, in that all are designed for classes whose instances are
 * potentially executed by another thread. A Runnable, however, cannot throw a checked exception, while
 * a Callable must return a value.</p>
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
@FunctionalInterface
public interface ThrowingCallable {

    /**
     * Invokes the task the implementation provides.
     *
     * @throws Exception is the implementation throws one
     */
    void call() throws Exception;
}
