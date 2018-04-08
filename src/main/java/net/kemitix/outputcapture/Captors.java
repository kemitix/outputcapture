package net.kemitix.outputcapture;

interface Captors {

    static SynchronousOutputCapturer syncRedirect() {
        return new SynchronousOutputCapturer(ThreadFilteredRedirectRouter::new);
    }

    static SynchronousOutputCapturer syncCopy() {
        return new SynchronousOutputCapturer(ThreadFilteredCopyRouter::new);
    }

    static AsynchronousOutputCapturer asyncRedirectThread() {
        return new AsynchronousOutputCapturer(ThreadFilteredRedirectRouter::new);
    }

    static AsynchronousOutputCapturer asyncCopyThread() {
        return new AsynchronousOutputCapturer(ThreadFilteredCopyRouter::new);
    }

    static AsynchronousOutputCapturer asyncRedirectAll() {
        return new AsynchronousOutputCapturer(PromiscuousRedirectRouter::new);
    }

    static AsynchronousOutputCapturer asyncCopyAll() {
        return new AsynchronousOutputCapturer(PromiscuousCopyRouter::new);
    }
}
