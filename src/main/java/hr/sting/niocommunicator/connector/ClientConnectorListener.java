package hr.sting.niocommunicator.connector;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Implementor of this interface will receive notification when TCP SocketChannel is connected ready for use.
 *
 * @author Marko Stipanov
 */
public interface ClientConnectorListener {
    /**
     * Called when client connector successfully established client connection.
     *
     * @param source of the event
     * @param sc     client connection socket channel
     */
    void onTcpClientConnect(ClientConnector source, SocketChannel sc) throws IOException;

    /**
     * Called when client connector could not establish client connection.
     *
     * @param source  of event
     * @param context
     */
    void onTcpClientConnectFailed(ClientConnector source, Object context);
}
