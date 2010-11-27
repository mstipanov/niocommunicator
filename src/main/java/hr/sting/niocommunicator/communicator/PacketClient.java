package hr.sting.niocommunicator.communicator;

import hr.sting.niocommunicator.Startable;

import java.net.InetSocketAddress;

/**
 * @author Marko Stipanov
 * @since 08.10.2010. 20:32:43
 */
public interface PacketClient extends Startable {
    void connect(InetSocketAddress inetSocketAddress, Object context);
}
