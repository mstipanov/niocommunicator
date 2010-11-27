package hr.sting.niocommunicator.channel;

/**
 * @author Marko Stipanov
 */
public class PacketChannelEvent<T extends Packet> implements Runnable {

    /**
     * Instance that will be used to process associated {@link #data}
     */
    private PacketChannel<T> packetChannel;

    /**
     * data which will be processed by associated {@link #packetChannel} instance
     */
    private byte[] data;

    public PacketChannelEvent(PacketChannel<T> packetChannel, byte[] data) {
        this.packetChannel = packetChannel;
        this.data = data;
    }

    /**
     * Calls {@link PacketChannel#processData(byte[])} specifying associated {@link #data} as argument.
     */
    public void run() {
        packetChannel.processData(data);
    }

}
