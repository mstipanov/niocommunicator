/*
  (c) 2004, Nuno Santos, nfsantos@sapo.pt
  released under terms of the GNU public license
  http://www.gnu.org/licenses/licenses.html#TOCGPL
*/
package hr.sting.niocommunicator;

/**
 * Interface used for reading and writing from a socket using
 * non-blocking operations.
 * <p/>
 * Classes wishing to be notified when a socket is ready to be written
 * or read should implement this interface in order to receive
 * notifications.
 *
 * @author Nuno Santos
 */
public interface ReadWriteSelectorHandler extends SelectorHandler {

    /**
     * Called when the associated socket is ready to be read.
     */
    public void handleRead();

    /**
     * Called when the associated socket is ready to be written.
     */
    public void handleWrite();
}