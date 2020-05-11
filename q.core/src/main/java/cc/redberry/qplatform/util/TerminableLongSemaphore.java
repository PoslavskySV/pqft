package cc.redberry.qplatform.util;

public final class TerminableLongSemaphore {
    /** The state: number of available availablePermits, or -1 if object is closed */
    private long availablePermits = 0;

    /**
     * Releases permits to be used by consuming threads
     *
     * @param permits number of permits to add
     * @return true if successfully added permits, false if the latch is already terminated
     */
    public synchronized boolean release(long permits) {
        if (availablePermits == -1)
            return false;
        availablePermits += permits;
        this.notifyAll();
        return true;
    }

    /**
     * Acquire at most maxPermits permits, wait if 0 permits available unit at least one will be available.
     *
     * @param maxPermits maximum permits to acquire
     */
    public synchronized long acquire(long maxPermits) throws InterruptedException {
        while (availablePermits == 0)
            this.wait();
        if (availablePermits == -1)
            return -1;
        long acquiredPermits = Math.min(maxPermits, availablePermits);
        availablePermits -= acquiredPermits;
        return acquiredPermits;
    }

    /**
     * Terminate the semaphore. After calling this method, acquire will always return -1.
     */
    public synchronized void terminate() {
        availablePermits = -1;
        this.notifyAll();
    }
}
