package hr.sting.niocommunicator.channel;

import hr.sting.niocommunicator.serialization.ByteArraySerializer;

/**
 * @author Marko Stipanov
 * @since 08.10.2010. 21:18:53
 */
public interface PacketAssemblerFactory<T extends Packet> {
    PacketAssembler<T> create();

    ByteArraySerializer getByteArraySerializer();

    void setByteArraySerializer(ByteArraySerializer byteArraySerializer);
}
