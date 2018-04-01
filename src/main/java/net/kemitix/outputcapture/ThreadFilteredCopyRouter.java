package net.kemitix.outputcapture;

import lombok.Getter;

class ThreadFilteredCopyRouter implements CopyRouter, ThreadFilteredRouter {

    @Getter
    private final Thread filteringThread;

    ThreadFilteredCopyRouter(final RouterParameters routerParameters) {
        filteringThread = routerParameters.getFilteringThread();
    }
}
