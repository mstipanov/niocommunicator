/*
  (c) 2004, Nuno Santos, nfsantos@sapo.pt
  released under terms of the GNU public license
  http://www.gnu.org/licenses/licenses.html#TOCGPL
*/
package hr.sting.niocommunicator;

/**
 * Interface used for accepting incoming connections using non-blocking
 * operations.
 * <p/>
 * Classes wishing to be notified when a ServerSocket receives incoming
 * connections should implement this interface.
 *
 * @author Nuno Santos
 */
public interface AcceptSelectorHandler extends SelectorHandler {
    /**
     * Called by SelectorThread when the server socket associated
     * with the class implementing this interface receives a request
     * for establishing a connection.
     */
    public void handleAccept();
}
