package hr.sting.niocommunicator.channel;

/**
 * @author Marko Stipanov
 * @since 08.10.2010. 22:34:36
 */
public class DelegatingPacketChannelListener<T extends Packet> implements PacketChannelListener<T> {
    private PacketChannelListener<T> listener;

    public DelegatingPacketChannelListener(PacketChannelListener<T> listener) {
        this.listener = listener;
    }

    @Override
    public void onSocketNotReadyForWrite() {
        listener.onSocketNotReadyForWrite();
    }

    @Override
    public void onSocketReadyForWrite() {
        listener.onSocketReadyForWrite();
    }

    public void packetArrived(PacketChannel<T> packetChannel, T packet) {
        listener.packetArrived(packetChannel, packet);
    }

    public void packetSent(PacketChannel<T> tPacketChannel, T packet) {
        listener.packetSent(tPacketChannel, packet);
    }

    public void socketConnected(PacketChannel<T> tPacketChannel, Object context) {
        listener.socketConnected(tPacketChannel, context);
    }

    public void socketDisconnected(PacketChannel<T> tPacketChannel) {
        listener.socketDisconnected(tPacketChannel);
    }

    public void socketException(PacketChannel<T> tPacketChannel, Exception ex) {
        listener.socketException(tPacketChannel, ex);
    }
}
