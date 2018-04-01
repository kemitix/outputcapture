package net.kemitix.outputcapture;

interface ThreadFilteredRouter extends Router {

    @Override
    default boolean accepts(Byte aByte) {
        final Thread currentThread = Thread.currentThread();
        final Thread targetThread = getFilteringThread();
        if (currentThread.equals(targetThread)) return true;

        final ThreadGroup targetThreadGroup = targetThread.getThreadGroup();
        final ThreadGroup currentThreadGroup = currentThread.getThreadGroup();
        if (currentThreadGroup == null || targetThreadGroup == null) return false;

        return currentThread.equals(targetThread) ||
                targetThreadGroup.parentOf(currentThreadGroup);
    }

    /**
     * The thread to filter on.
     *
     * @return a Thread
     */
    Thread getFilteringThread();
}
