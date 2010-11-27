/*
  (c) 2004, Nuno Santos, nfsantos@sapo.pt
  released under terms of the GNU public license
  http://www.gnu.org/licenses/licenses.html#TOCGPL
*/
package hr.sting.niocommunicator.handler;

import java.nio.channels.SocketChannel;

/**
 * Callback interface for receiving events from an Acceptor.
 *
 * @author Nuno Santos
 */
public interface AcceptorListener {
    /**
     * Called when a connection is established.
     *
     * @param acceptor The acceptor that originated this event.
     * @param sc       The newly connected socket.
     */
    public void socketConnected(Acceptor acceptor, SocketChannel sc);

    /**
     * Called when an error occurs on the Acceptor.
     *
     * @param acceptor The acceptor where the error occurred.
     * @param ex       The exception representing the error.
     */
    public void socketError(Acceptor acceptor, Exception ex);
}
