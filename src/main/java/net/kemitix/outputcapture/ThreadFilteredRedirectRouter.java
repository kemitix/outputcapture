package net.kemitix.outputcapture;

import lombok.Getter;

class ThreadFilteredRedirectRouter implements RedirectRouter, ThreadFilteredRouter {

    @Getter
    private final Thread filteringThread;

    ThreadFilteredRedirectRouter(final RouterParameters routerParameters) {
        filteringThread = routerParameters.getFilteringThread();
    }
}
