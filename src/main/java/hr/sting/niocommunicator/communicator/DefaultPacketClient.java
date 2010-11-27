package hr.sting.niocommunicator.communicator;

import hr.sting.niocommunicator.SelectorThread;
import hr.sting.niocommunicator.channel.Packet;
import hr.sting.niocommunicator.channel.PacketAssemblerFactory;
import hr.sting.niocommunicator.channel.PacketChannelEventProcessor;
import hr.sting.niocommunicator.channel.PacketChannelListener;
import hr.sting.niocommunicator.connector.ClientConnector;
import hr.sting.niocommunicator.connector.ClientConnectorListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @author Marko Stipanov
 * @since 08.10.2010. 20:34:37
 */
public class DefaultPacketClient<T extends Packet> extends AbstractPacketCommunicator<T> implements PacketClient, ClientConnectorListener {
    private static final Log LOGGER = LogFactory.getLog(DefaultPacketClient.class);

    public DefaultPacketClient(PacketAssemblerFactory<T> packetAssemblerFactory, PacketChannelListener<T> packetChannelListener, PacketChannelEventProcessor<T> packetChannelEventProcessor) throws IOException {
        super(packetAssemblerFactory, packetChannelListener, packetChannelEventProcessor);
    }

    public DefaultPacketClient(SelectorThread selector, PacketAssemblerFactory<T> packetAssemblerFactory, PacketChannelListener<T> packetChannelListener, PacketChannelEventProcessor<T> packetChannelEventProcessor) {
        super(packetAssemblerFactory, packetChannelListener, selector, packetChannelEventProcessor);
    }

    @Override
    public void connect(InetSocketAddress inetSocketAddress, Object context) {
        ClientConnector clientConnector = new ClientConnector(selector, inetSocketAddress, this, context);
        clientConnector.connect();
    }

    @Override
    public void onTcpClientConnect(ClientConnector clientConnector, SocketChannel sc) throws IOException {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Client connected: " + sc.socket().getInetAddress());

        channelConnected(sc, clientConnector.getContext());
    }

    @Override
    public void onTcpClientConnectFailed(ClientConnector source, Object context) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Client connect failed: " + source);
    }
}
