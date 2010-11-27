package hr.sting.niocommunicator.connector;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface ServerConnectorListener {

    void onTcpServerConnect(ServerConnector source, SocketChannel sc) throws IOException;

}
