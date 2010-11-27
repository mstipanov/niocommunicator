/*
  (c) 2004, Nuno Santos, nfsantos@sapo.pt
  released under terms of the GNU public license
  http://www.gnu.org/licenses/licenses.html#TOCGPL
*/
package hr.sting.niocommunicator.handler;

import hr.sting.niocommunicator.AcceptSelectorHandler;
import hr.sting.niocommunicator.CallbackErrorHandler;
import hr.sting.niocommunicator.SelectorThread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Listens for incoming connections from clients, using a selector
 * to receive connect events. Therefore, instances of this class
 * don't have an associated thread. When a connection is established,
 * it notifies a listener using a callback.
 *
 * @author Nuno Santos
 */
final public class Acceptor implements AcceptSelectorHandler {
    // Used to receive incoming connections
    private ServerSocketChannel ssc;

    // The address containing the port where to listen for connections.
    private final InetSocketAddress serverAddress;

    // The selector used by this instance.
    private final SelectorThread ioThread;

    // Listener to be notified of new connections and of errors.
    private final AcceptorListener listener;

    /**
     * Creates a new instance. No server socket is created. Use
     * openServerSocket() to start listening.
     *
     * @param serverAddress The address containing the port to open.
     * @param listener      The object that will receive notifications
     *                      of incoming connections.
     * @param ioThread      The selector thread.
     */
    public Acceptor(
            InetSocketAddress serverAddress,
            SelectorThread ioThread,
            AcceptorListener listener) {
        this.serverAddress = serverAddress;
        this.ioThread = ioThread;
        this.listener = listener;
    }

    /**
     * Starts listening for incoming connections. This method does
     * not block waiting for connections. Instead, it registers itself
     * with the selector to receive connect events.
     *
     * @throws IOException on socket open error
     */
    public void openServerSocket() throws IOException {
        ssc = ServerSocketChannel.open();
        ssc.socket().setReuseAddress(true);
        ssc.socket().bind(serverAddress, 100);


        // This method might be called from any thread. We must use
        // the xxxLater methods so that the actual register operation
        // is done by the selector's thread. No other thread should access
        // the selector directly.
        ioThread.registerChannelLater(ssc,
                SelectionKey.OP_ACCEPT,
                this,
                new CallbackErrorHandler() {
                    public void handleError(Exception ex) {
                        listener.socketError(Acceptor.this, ex);
                    }
                });
    }

    public String toString() {
        return "ListenPort: " + serverAddress.getPort();
    }

    /**
     * Called by SelectorThread when the underlying server socket is
     * ready to accept a connection. This method should not be called
     * from anywhere else.
     */
    public void handleAccept() {
        SocketChannel sc = null;
        try {
            sc = ssc.accept();

            // Reactivate interest to receive the next connection. We
            // can use one of the XXXNow methods since this method is being
            // executed on the selector's thread.
            ioThread.addChannelInterestNow(ssc, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            listener.socketError(this, e);
        }
        if (sc != null) {
            // Connection established
            listener.socketConnected(this, sc);
        }
    }

    /**
     * Closes the socket. Returns only when the socket has been
     * closed.
     */
    public void close() {
        try {
            // Must wait for the socket to be closed.
            ioThread.invokeAndWait(new Runnable() {
                public void run() {
                    if (ssc != null) {
                        try {
                            ssc.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            });
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}