package net.kemitix.outputcapture;

interface ThreadFilteredRouter extends Router {

    @Override
    default boolean accepts(Byte aByte) {
        final Thread currentThread = Thread.currentThread();
        final Thread targetThread = getFilteringThread();
        return currentThread.equals(targetThread) ||
                targetThread.getThreadGroup().parentOf(currentThread.getThreadGroup());
    }

    /**
     * The thread to filter on.
     *
     * @return a Thread
     */
    Thread getFilteringThread();
}
