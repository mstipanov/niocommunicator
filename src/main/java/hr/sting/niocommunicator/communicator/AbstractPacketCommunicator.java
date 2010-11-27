package hr.sting.niocommunicator.communicator;

import hr.sting.niocommunicator.SelectorThread;
import hr.sting.niocommunicator.Startable;
import hr.sting.niocommunicator.channel.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Marko Stipanov
 * @since 08.10.2010. 20:34:01
 */
public abstract class AbstractPacketCommunicator<T extends Packet> implements Startable {
    private static final Log LOGGER = LogFactory.getLog(AbstractPacketCommunicator.class);

    protected final PacketChannelEventProcessor<T> packetChannelEventProcessor;

    protected final SelectorThread selector;
    protected final PacketChannelListener<T> packetChannelListener;
    protected final PacketAssemblerFactory<T> packetAssemblerFactory;

    protected final ConcurrentLinkedQueue<PacketChannel<T>> connectedChannels = new ConcurrentLinkedQueue<PacketChannel<T>>();

    protected AbstractPacketCommunicator(PacketAssemblerFactory<T> packetAssemblerFactory, PacketChannelListener<T> packetChannelListener, PacketChannelEventProcessor<T> packetChannelEventProcessor) throws IOException {
        this(packetAssemblerFactory, packetChannelListener, new SelectorThread(), packetChannelEventProcessor);
    }

    protected AbstractPacketCommunicator(PacketAssemblerFactory<T> packetAssemblerFactory, PacketChannelListener<T> packetChannelListener, SelectorThread selector, PacketChannelEventProcessor<T> packetChannelEventProcessor) {
        this.packetAssemblerFactory = packetAssemblerFactory;
        this.packetChannelListener = new DelegatingPacketChannelListener<T>(packetChannelListener) {
            @Override
            public void socketDisconnected(PacketChannel<T> packetChannel) {
                connectedChannels.remove(packetChannel);
                packetChannel.stop();
                super.socketDisconnected(packetChannel);
            }
        };
        this.selector = selector;
        this.packetChannelEventProcessor = packetChannelEventProcessor;
    }

    @Override
    public void start() throws IOException {
        selector.start();
        packetChannelEventProcessor.start();
    }

    @Override
    public void stop() throws IOException {
        packetChannelEventProcessor.stop();
        selector.stop();

        for (PacketChannel<T> connectedChannel : connectedChannels) {
            connectedChannel.stop();
            connectedChannels.remove();
        }
    }

    protected void channelConnected(SocketChannel sc, Object context) throws IOException {
        PacketAssembler<T> packetAssembler = packetAssemblerFactory.create();
        PacketChannel<T> packetChannel = new PacketChannel<T>(selector, sc, packetChannelListener, packetChannelEventProcessor, packetAssembler);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Packet channel created: " + packetChannel);
        packetChannelListener.socketConnected(packetChannel, context);
        connectedChannels.add(packetChannel);
        packetChannel.start();
    }

}
