/*
  (c) 2004, Nuno Santos, nfsantos@sapo.pt
  released under terms of the GNU public license
  http://www.gnu.org/licenses/licenses.html#TOCGPL
*/
package hr.sting.niocommunicator;

import hr.sting.niocommunicator.worker.AbstractLoopWorker;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Event process for I/O events raised by a selector. This class receives the
 * lower level events raised by a Selector and dispatches them to the
 * appropriate handler. It also manages all other operations on the selector,
 * like registering and un-registering channels, or updating the events of
 * interest for each monitored socket.
 * <p/>
 * This class is inspired on the java.awt.EventQueue and follows a similar
 * model. The EventQueue class is responsible for making sure that all
 * operations on AWT objects are performed on a single thread, the one managed
 * internally by EventQueue. The SelectorThread class performs a similar
 * task. In particular:
 * <p/>
 * - Only the thread created by instances of this class should be allowed
 * to access the selector and all sockets managed by it. This means that
 * all I/O operations on the sockets should be performed on the corresponding
 * selector's thread. If some other thread wants to access objects managed
 * by this selector, then it should use <code>invokeLater()</code> or the
 * <code>invokeAndWait()</code> to dispatch a runnable to this thread.
 * <p/>
 * - This thread should not be used to perform lengthy operations. In
 * particular, it should never be used to perform blocking I/O operations.
 * To perform a time consuming task use a worker thread.
 * <p/>
 * <p/>
 * This architecture is required for two main reasons:
 * <p/>
 * The first, is to make synchronization in the objects of a connection
 * unnecessary. This is good for performance and essential for keeping
 * the complexity low. Getting synchronization right within the objects
 * of a connection would be extremely tricky.
 * <p/>
 * The second is to make sure that all actions over the selector, its
 * keys and related sockets are carried in the same thread. My personal
 * experience with selectors is that they don't work well when being
 * accessed concurrently by several threads. This is mostly the result
 * of bugs in some of the version of Sun's Java SDK (these problems were
 * found with version 1.4.2_02). Some of the bugs have already been
 * identified and fixed by Sun. But it is better to work around them
 * by avoiding multi-threaded access to the selector.
 *
 * @author Nuno Santos
 */
public class SelectorThread extends AbstractLoopWorker {
    /**
     * Selector used for I/O multiplexing
     */
    private Selector selector;

    /**
     * The thread associated with this selector
     */
    private Thread selectorThread;

    /**
     * List of tasks to be executed in the selector thread.
     * Submitted using invokeLater() and executed in the main
     * select loop.
     */
    private final LinkedList<Runnable> pendingInvocations = new LinkedList<Runnable>();

    /**
     * Creates a new selector and the associated thread. The thread
     * is started by this constructor, thereby making this object
     * ready to be used.
     *
     * @throws IOException on selector error
     */
    public SelectorThread() throws IOException {
        // Selector for incoming time requests
//        selector = Selector.open();
        selector = SelectorProvider.provider().openSelector();
        // start();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public boolean stop(int timeout) {
        selector.wakeup();
        return super.stop(timeout);
    }

    /**
     * Adds a new interest to the list of events where a channel is
     * registered. This means that the associated event handler will
     * start receiving events for the specified interest.
     * <p/>
     * This method should only be called on the selector thread. Otherwise
     * an exception is thrown. Use the addChannelInterestLater() when calling
     * from another thread.
     *
     * @param channel  The channel to be updated. Must be registered.
     * @param interest The interest to add. Should be one of the
     *                 constants defined on SelectionKey.
     * @throws java.io.IOException on selector error
     */
    public synchronized void addChannelInterestNow(SelectableChannel channel,
                                                   int interest) throws IOException {
        if (Thread.currentThread() != selectorThread) {
            throw new IOException("Method can only be called from selector thread");
        }
        SelectionKey sk = channel.keyFor(selector);
        if (null != sk) {
            changeKeyInterest(sk, sk.interestOps() | interest);
        }
    }

    /**
     * Like addChannelInterestNow(), but executed asynchronously on the
     * selector thread. It returns after scheduling the task, without
     * waiting for it to be executed.
     *
     * @param channel      The channel to be updated. Must be registered.
     * @param interest     The new interest to add. Should be one of the
     *                     constants defined on SelectionKey.
     * @param errorHandler Callback used if an exception is raised when executing the task.
     */
    public void addChannelInterestLater(final SelectableChannel channel,
                                        final int interest,
                                        final CallbackErrorHandler errorHandler) {
        // Add a new runnable to the list of tasks to be executed in the selector thread
        invokeLater(new Runnable() {
            public void run() {
                try {
                    addChannelInterestNow(channel, interest);
                } catch (IOException e) {
                    errorHandler.handleError(e);
                }
            }
        });
    }

    /**
     * Removes an interest from the list of events where a channel is
     * registered. The associated event handler will stop receiving events
     * for the specified interest.
     * <p/>
     * This method should only be called on the selector thread. Otherwise
     * an exception is thrown. Use the removeChannelInterestLater() when calling
     * from another thread.
     *
     * @param channel  The channel to be updated. Must be registered.
     * @param interest The interest to be removed. Should be one of the
     *                 constants defined on SelectionKey.
     * @throws java.io.IOException on selector error
     */
    public void removeChannelInterestNow(SelectableChannel channel,
                                         int interest) throws IOException {
        if (Thread.currentThread() != selectorThread) {
            throw new IOException("Method can only be called from selector thread");
        }
        SelectionKey sk = channel.keyFor(selector);
        changeKeyInterest(sk, sk.interestOps() & ~interest);
    }

    /**
     * Like removeChannelInterestNow(), but executed asynchronously on
     * the selector thread. This method returns after scheduling the task,
     * without waiting for it to be executed.
     *
     * @param channel      The channel to be updated. Must be registered.
     * @param interest     The interest to remove. Should be one of the
     *                     constants defined on SelectionKey.
     * @param errorHandler Callback used if an exception is raised when
     *                     executing the task.
     */
    public void removeChannelInterestLater(final SelectableChannel channel,
                                           final int interest,
                                           final CallbackErrorHandler errorHandler) {
        invokeLater(new Runnable() {
            public void run() {
                try {
                    removeChannelInterestNow(channel, interest);
                } catch (IOException e) {
                    errorHandler.handleError(e);
                }
            }
        });
    }

    /**
     * Updates the interest set associated with a selection key. The
     * old interest is discarded, being replaced by the new one.
     *
     * @param sk          The key to be updated.
     * @param newInterest The new key interest
     * @throws IOException on selector error
     */
    private void changeKeyInterest(SelectionKey sk,
                                   int newInterest) throws IOException {
        /* This method might throw two unchecked exceptions:
        * 1. IllegalArgumentException  - Should never happen. It is a bug if it happens
        * 2. CancelledKeyException - Might happen if the channel is closed while
        * a packet is being dispatched.
        */
        try {
            sk.interestOps(newInterest);
        } catch (CancelledKeyException cke) {
            IOException ioe = new IOException("Failed to change channel interest.");
            ioe.initCause(cke);
            throw ioe;
        }
    }

    /**
     * Like registerChannelLater(), but executed asynchronously on the
     * selector thread. It returns after scheduling the task, without
     * waiting for it to be executed.
     *
     * @param channel       The channel to be monitored.
     * @param selectionKeys The interest set. Should be a combination of
     *                      SelectionKey constants.
     * @param handlerInfo   The handler for events raised on the registered channel.
     * @param errorHandler  Used for asynchronous error handling.
     */
    public void registerChannelLater(final SelectableChannel channel,
                                     final int selectionKeys,
                                     final SelectorHandler handlerInfo,
                                     final CallbackErrorHandler errorHandler) {
        invokeLater(new Runnable() {
            public void run() {
                try {
                    registerChannelNow(channel, selectionKeys, handlerInfo);
                } catch (IOException e) {
                    errorHandler.handleError(e);
                }
            }
        });
    }

    /**
     * Registers a SelectableChannel with this selector. This channel will
     * start to be monitored by the selector for the set of events associated
     * with it. When an event is raised, the corresponding handler is
     * called.
     * <p/>
     * This method can be called multiple times with the same channel
     * and selector. Subsequent calls update the associated interest set
     * and selector handler to the ones given as arguments.
     * <p/>
     * This method should only be called on the selector thread. Otherwise
     * an exception is thrown. Use the registerChannelLater() when calling
     * from another thread.
     *
     * @param channel       The channel to be monitored.
     * @param selectionKeys The interest set. Should be a combination of
     *                      SelectionKey constants.
     * @param handlerInfo   The handler for events raised on the registered channel.
     * @throws java.io.IOException on communication error
     */
    public void registerChannelNow(SelectableChannel channel,
                                   int selectionKeys,
                                   SelectorHandler handlerInfo) throws IOException {
        if (Thread.currentThread() != selectorThread) {
            throw new IOException("Method can only be called from selector thread");
        }

        if (!channel.isOpen()) {
            throw new IOException("Channel is not open.");
        }

        try {
            if (channel.isRegistered()) {
                SelectionKey sk = channel.keyFor(selector);
                assert sk != null : "Channel is already registered with other selector";
                sk.interestOps(selectionKeys);
                Object previousAttach = sk.attach(handlerInfo);
                assert previousAttach != null;
            } else {
                channel.configureBlocking(false);
                channel.register(selector, selectionKeys, handlerInfo);
            }
        } catch (Exception e) {
            throw new IOException("Error registering channel.", e);
        }
    }

    /**
     * Executes the given task in the selector thread. This method returns
     * as soon as the task is scheduled, without waiting for it to be
     * executed.
     *
     * @param run The task to be executed.
     */
    public void invokeLater(Runnable run) {
        synchronized (pendingInvocations) {
            pendingInvocations.add(run);
        }
        selector.wakeup();
    }

    /**
     * Executes the given task synchronously in the selector thread. This
     * method schedules the task, waits for its execution and only then
     * returns.
     *
     * @param task The task to be executed on the selector's thread.
     * @throws InterruptedException when selector thread is interrupted
     */
    public void invokeAndWait(final Runnable task)
            throws InterruptedException {
        if (Thread.currentThread() == selectorThread) {
            // We are in the selector's thread. No need to schedule
            // execution
            task.run();
        } else {
            // Used to deliver the notification that the task is executed
            final CountDownLatch latch = new CountDownLatch(1);
            // Uses the invokeLater method with a newly created task
            this.invokeLater(new Runnable() {
                public void run() {
                    task.run();
                    // Notifies
                    latch.countDown();
                }
            });
            // Wait for the task to complete.
            latch.await();
            // Ok, we are done, the task was executed. Proceed.
        }
    }

    /**
     * Executes all tasks queued for execution on the selector's thread.
     * <p/>
     * Should be called holding the lock to <code>pendingInvocations</code>.
     */
    private void doInvocations() {
        synchronized (pendingInvocations) {
            for (Runnable task : pendingInvocations) {
                task.run();
            }
            pendingInvocations.clear();
        }
    }

    /**
     * Main cycle. This is where event processing and
     * dispatching happens.
     */
    @Override
    public void work() {
        if (null == selectorThread) {
            selectorThread = Thread.currentThread();
        }

        try {
            // Execute all the pending tasks.
            doInvocations();
        } catch (Exception e) {
            LOGGER.error("error in doInvocations: " + e.getMessage(), e);
            return;
        }

        int selectedKeys;
        try {
            selectedKeys = selector.select();
        } catch (IOException ioe) {
            // Select should never throw an exception under normal
            // operation. If this happens, print the error and try to
            // continue working.
            // ioe.printStackTrace();
            LOGGER.error("error in select: " + ioe.getMessage(), ioe);
            return;
        }

        if (selectedKeys == 0) {
            // Go back to the beginning of the loop
            return;
        }

        // Someone is ready for IO, get the ready keys
        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
        // Walk through the collection of ready keys and dispatch
        // any active event.
        while (it.hasNext()) {
            SelectionKey sk = (SelectionKey) it.next();
            it.remove();
            try {
                // Obtain the interest of the key
                int readyOps = sk.readyOps();
                // Disable the interest for the operation that is ready.
                // This prevents the same event from being raised multiple
                // times.
                sk.interestOps(sk.interestOps() & ~readyOps);
                SelectorHandler handler =
                        (SelectorHandler) sk.attachment();

                // Some of the operations set in the selection key
                // might no longer be valid when the handler is executed.
                // So handlers should take precautions against this
                // possibility.

                // Check what are the interests that are active and
                // dispatch the event to the appropriate method.
                if (sk.isAcceptable()) {
                    // A connection is ready to be completed
                    ((AcceptSelectorHandler) handler).handleAccept();

                } else if (sk.isConnectable()) {
                    // A connection is ready to be accepted
                    ((ConnectorSelectorHandler) handler).handleConnect();

                } else {
                    ReadWriteSelectorHandler rwHandler =
                            (ReadWriteSelectorHandler) handler;
                    // Readable or writable
                    if (sk.isReadable()) {
                        // It is possible to read
                        rwHandler.handleRead();
                    }

                    // Check if the key is still valid, since it might
                    // have been invalidated in the read handler
                    // (for instance, the socket might have been closed)
                    if (sk.isValid() && sk.isWritable()) {
                        // It is read to write
                        rwHandler.handleWrite();
                    }
                }
            } catch (Throwable t) {

                LOGGER.error("exception while processing selectionKey, channel=" + sk.channel() + ", isValid=" + sk.isValid() + ", error=" + t.getMessage(), t);

                // t.printStackTrace();

                // No exceptions should be thrown in the previous block!
                // So kill everything if one is detected.
                // Makes debugging easier.
                // -- mz: commented out so abstract processor does not die
//                closeSelectorAndChannels();
//                t.printStackTrace();

                return;
            }
        }
    }

    /**
     * Closes all channels registered with the selector. Used to
     * clean up when the selector dies and cannot be recovered.
     */
    private void closeSelectorAndChannels() {
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            try {
                key.channel().close();
            } catch (IOException e) {
                // Ignore
            }
        }
        try {
            selector.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}