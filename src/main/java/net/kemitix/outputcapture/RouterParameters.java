package net.kemitix.outputcapture;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
class RouterParameters {

    private final Thread filteringThread;

    static RouterParameters createDefault() {
        return new RouterParameters(Thread.currentThread());
    }
}
