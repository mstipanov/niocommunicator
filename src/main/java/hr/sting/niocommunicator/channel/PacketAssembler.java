package hr.sting.niocommunicator.channel;

import java.util.List;

/**
 * @author Marko Stipanov
 * @since 12.02.2010. 11:37:02
 */
public interface PacketAssembler<T extends Packet> {
    List<T> appendReceivedData(PacketChannel<T> packetChannel, byte[] data);
}