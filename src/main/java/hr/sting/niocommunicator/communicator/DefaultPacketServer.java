package hr.sting.niocommunicator.communicator;

import hr.sting.niocommunicator.SelectorThread;
import hr.sting.niocommunicator.channel.Packet;
import hr.sting.niocommunicator.channel.PacketAssemblerFactory;
import hr.sting.niocommunicator.channel.PacketChannelEventProcessor;
import hr.sting.niocommunicator.channel.PacketChannelListener;
import hr.sting.niocommunicator.connector.ServerConnector;
import hr.sting.niocommunicator.connector.ServerConnectorListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @author Marko Stipanov
 * @since 08.10.2010. 20:26:17
 */
public class DefaultPacketServer<T extends Packet> extends AbstractPacketCommunicator<T> implements PacketServer, ServerConnectorListener {
    private static final Log LOGGER = LogFactory.getLog(DefaultPacketServer.class);
    private ServerConnector serverConnector;
    private InetSocketAddress serverAddress;

    public DefaultPacketServer(InetSocketAddress serverAddress, PacketAssemblerFactory<T> packetAssemblerFactory, PacketChannelListener<T> packetChannelListener, PacketChannelEventProcessor<T> packetChannelEventProcessor) throws IOException {
        super(packetAssemblerFactory, packetChannelListener, packetChannelEventProcessor);
        this.serverAddress = serverAddress;
    }

    public DefaultPacketServer(SelectorThread selector, InetSocketAddress serverAddress, PacketAssemblerFactory<T> packetAssemblerFactory, PacketChannelListener<T> packetChannelListener, PacketChannelEventProcessor<T> packetChannelEventProcessor) {
        super(packetAssemblerFactory, packetChannelListener, selector, packetChannelEventProcessor);
        this.serverAddress = serverAddress;
    }

    @Override
    public void start() throws IOException {
        super.start();

        serverConnector = new ServerConnector(selector, serverAddress, this);
        serverConnector.start();
    }

    @Override
    public void stop() throws IOException {
        serverConnector.stop();
        super.stop();
    }

    @Override
    public void onTcpServerConnect(ServerConnector source, SocketChannel sc) throws IOException {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Client arrived: " + sc.socket().getInetAddress());

        channelConnected(sc, null);
    }
}
