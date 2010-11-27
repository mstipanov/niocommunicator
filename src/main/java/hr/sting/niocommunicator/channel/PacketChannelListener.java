/*
  (c) 2004, Nuno Santos, nfsantos@sapo.pt
  released under terms of the GNU public license
  http://www.gnu.org/licenses/licenses.html#TOCGPL
*/
package hr.sting.niocommunicator.channel;

/**
 * @author Marko Stipanov
 */
public interface PacketChannelListener<T extends Packet> {
    /**
     * Called when the socket was connected.
     *
     * @param packetChannel The source of the event.
     * @param context
     */
    public void socketConnected(PacketChannel<T> packetChannel, Object context);


    /**
     * Called when a packet is fully reassembled.
     *
     * @param packetChannel The source of the event.
     * @param packet        The reassembled packet
     * @throws java.io.IOException on communication error
     */
    public void packetArrived(PacketChannel<T> packetChannel, T packet);

    /**
     * Called after finishing sending a packet.
     *
     * @param packetChannel The source of the event.
     * @param packet        The packet sent
     */
    public void packetSent(PacketChannel<T> packetChannel, T packet);

    /**
     * Called when some error occurs while reading or writing to
     * the socket.<br>
     * <br>
     * Underlying socket channel will be closed prior to this call.
     *
     * @param packetChannel The source of the event.
     * @param ex            The exception representing the error.
     */
    public void socketException(PacketChannel<T> packetChannel, Exception ex);

    /**
     * Called when the read operation reaches the end of stream. This
     * means that the socket was closed.<br>
     * <br>
     * Underlying socket channel will be closed prior to this call.
     *
     * @param packetChannel The source of the event.
     */
    public void socketDisconnected(PacketChannel<T> packetChannel);

    public void onSocketReadyForWrite();

    public void onSocketNotReadyForWrite();
}
