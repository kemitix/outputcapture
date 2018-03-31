package net.kemitix.outputcapture;

interface CopyRouter extends Router {

    @Override
    default boolean isBlocking() {
        return false;
    }
}
