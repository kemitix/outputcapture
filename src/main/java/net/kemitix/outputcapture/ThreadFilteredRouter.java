package net.kemitix.outputcapture;

interface ThreadFilteredRouter extends Router {

    @Override
    default boolean accepts(Byte aByte) {
        return Thread.currentThread().equals(getFilteringThread());
    }

    /**
     * The thread to filter on.
     *
     * @return a Thread
     */
    Thread getFilteringThread();
}
