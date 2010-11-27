/*
  (c) 2004, Nuno Santos, nfsantos@sapo.pt
  released under terms of the GNU public license
  http://www.gnu.org/licenses/licenses.html#TOCGPL
*/
package hr.sting.niocommunicator;

/**
 * Defines the callback to be used to handle errors in
 * asynchronous method invocations.
 * <p/>
 * When an operation is executed asynchronously it is not
 * possible to use exceptions for error handling. Instead,
 * the caller must provide a callback to be used in case
 * of error.
 *
 * @author Nuno Santos
 */
public interface CallbackErrorHandler {
    /**
     * Called when an exception is raised when executing an
     * asynchronous method.
     *
     * @param ex raised exception
     */
    public void handleError(Exception ex);
}
