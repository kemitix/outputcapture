package net.kemitix.outputcapture;

interface PromiscuousRouter extends Router {

    @Override
    default boolean accepts(final Byte aByte) {
        return true;
    }
}
