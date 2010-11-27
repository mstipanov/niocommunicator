package hr.sting.niocommunicator.worker;

import hr.sting.niocommunicator.Startable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Looping worker. Subclasses implement the actual work.
 * Supports graceful stop.
 *
 * @author Marko Stipanov
 * @version 1.0
 */
public abstract class AbstractLoopWorker implements Runnable, Startable {
    public static final Log LOGGER = LogFactory.getLog(AbstractLoopWorker.class.getName());
    private static final int DEFAULT_STOP_ATTEMPTS = 3;

    private AtomicBoolean started = new AtomicBoolean(false);

    private String threadName = this.getClass().getSimpleName() + "-Thread";
    protected volatile Thread thread = null;

    public void start() {
        if (started.get())
            return;

        actualStart();
    }

    private synchronized void actualStart() {
        if (started.get())
            return;

        thread = new Thread(this);
        thread.setName(getThreadName() + "-" + thread.getId());

        started.set(true);
        thread.start();
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public void run() {
        Thread thisThread = Thread.currentThread();

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Thread " + thisThread.getName() + " started.");

        try {
            while (thread == thisThread) {
                work();
            }
        } catch (Throwable t) {
            LOGGER.error("Thread " + thisThread.getName() + " exception", t);
        }

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Thread " + thisThread.getName() + " ended.");

        started.set(false);
    }

    /**
     * Subclasses must implement the actual work.
     */
    protected abstract void work();

    /**
     * Started indicator.
     *
     * @return worker status.
     */
    public boolean isStarted() {
        return started.get();
    }

    protected boolean isRunning() {
        return thread != null;
    }

    protected Thread shutdown() {
        Thread thread = this.thread;
        this.thread = null;
        return thread;
    }

    /**
     * Stop gracefully.
     */
    public void stop() {
        stop(3000);
    }

    /**
     * Waits at most millis milliseconds for thread to stop.
     * A timeout of 0 will wait forever.
     *
     * @param timeout time to wait in milliseconds.
     * @return true if thread stopped or false otherwise.
     */
    public boolean stop(int timeout) {
        if (!isStarted())
            return true;

        Thread thread = shutdown();

        if (thread == null)
            return false;

        thread.interrupt();

        if (Thread.currentThread() == thread)
            return false;

        int attempts = DEFAULT_STOP_ATTEMPTS;
        int millis = timeout;
        if (millis >= attempts)
            millis /= attempts;

        for (int i = 0; i < attempts; i++) {
            try {
                thread.join(millis);
            } catch (Exception e) {
            }

            if (!isStarted())
                break;

            thread.interrupt();
        }

        if (isStarted()) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Thread " + getThreadName() + " failed to stop.");

            return false;
        }

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Thread " + getThreadName() + " stopped.");

        return true;
    }

}
