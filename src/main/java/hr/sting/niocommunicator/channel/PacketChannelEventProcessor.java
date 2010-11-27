package hr.sting.niocommunicator.channel;

import hr.sting.niocommunicator.Startable;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Marko Stipanov
 */
public class PacketChannelEventProcessor<T extends Packet> implements Startable {
    private static final int DEFAULT_POOL_SIZE = 2;

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(DEFAULT_POOL_SIZE, DEFAULT_POOL_SIZE, 5, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>());

    public void process(PacketChannelEvent<T> event) {
        executor.execute(event);
    }

    public void setPoolSize(int poolSize) {
        executor.setMaximumPoolSize(poolSize);
        executor.setCorePoolSize(poolSize);
    }

    public int getQueueSize() {
        return executor.getQueue().size();
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public void start() {
    }

    public void stop() {
        executor.shutdown();
    }
}
