package hr.sting.niocommunicator.channel;

import hr.sting.niocommunicator.CallbackErrorHandler;
import hr.sting.niocommunicator.ReadWriteSelectorHandler;
import hr.sting.niocommunicator.SelectorThread;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author Marko Stipanov
 */
public class PacketChannel<T extends Packet> implements ReadWriteSelectorHandler {
    private static final Log LOGGER = LogFactory.getLog(PacketChannel.class);

    private SelectorThread selector;
    private SocketChannel sc;
    private PacketChannelListener<T> listener;
    private PacketChannelEventProcessor<T> packetChannelEventProcessor;

    private ByteBuffer inBuffer;
    private ByteBuffer outBuffer;

    private PacketAssembler<T> packetAssembler;
    private CallbackErrorHandler writeReadyRequestCallbackErrorHandler = createWriteReadyRequestCallbackErrorHandler();
    private CallbackErrorHandler readReadyRequestCallbackErrorHandler = createReadReadyRequestCallbackErrorHandler();

    private AtomicBoolean opened = new AtomicBoolean(false);

    private InetSocketAddress remoteSocketAddress;

    /**
     * @param selector                    io thread which will provide asynchronous socket channel read/write ready notifications
     * @param sc                          socket channel for reading/writing data
     * @param listener                    to notify about PacketChannel events
     * @param packetChannelEventProcessor
     * @throws SocketException
     */
    public PacketChannel(SelectorThread selector, SocketChannel sc, PacketChannelListener<T> listener, PacketChannelEventProcessor<T> packetChannelEventProcessor, PacketAssembler<T> packetAssembler) throws SocketException {
        this.selector = selector;
        this.sc = sc;
        this.listener = listener;
        this.packetChannelEventProcessor = packetChannelEventProcessor;
        this.packetAssembler = packetAssembler;

        this.inBuffer = ByteBuffer.allocateDirect(sc.socket().getReceiveBufferSize());
        // TODO: 4k outbuffer
        this.outBuffer = ByteBuffer.allocateDirect(sc.socket().getSendBufferSize());
        this.outBuffer.clear();

        this.remoteSocketAddress = getRemoteSocketAddressFrom(this.sc);

        selector.registerChannelLater(sc, 0, this, readReadyRequestCallbackErrorHandler);
    }

    public void setPacketChannelListener(PacketChannelListener<T> listener) {
        this.listener = listener;
    }

    public void start() throws IOException {

        if (listener == null) {
            throw new IllegalStateException("listener is null - set valid listener");
        }

        if (!sc.isOpen()) {
            throw new IllegalStateException("specified socket channel is not open");
        }

        opened.set(true);

        requestReadReadyNotificationLater();
    }

    @Override
    public void handleRead() {
        // TODO: assemble read bytes into PDU and notify PduChannelListener about new packet

        try {
            // empty input buffer
            inBuffer.clear();

            // Reads from the socket
            // Returns -1 if it has reached end-of-stream
            int readBytes = sc.read(inBuffer);

            // End of stream???
            if (readBytes == -1) {
                // End of stream. Closing channel...
                // -- I think this is not necessary here... (lets see :)
                // selector.removeChannelInterestNow(sc, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                closeAndNotifySocketClosed();
                return;
            }

            // Nothing else to be read?
            if (readBytes == 0) {
                // There was nothing to read. Shouldn't happen often, but
                // it is not an error, we can deal with it. Ignore this event
                // and reactivate reading.

                selector.addChannelInterestNow(sc, SelectionKey.OP_READ);

                return;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Data read data from socket: " + sc + " - " + readBytes);
            }

            // this should always be true here
            assert readBytes == inBuffer.position() : "here, inBuffer must contain exactly 'readBytes' of data";

            byte[] data = toByteArray(readBytes, inBuffer);

            packetChannelEventProcessor.process(new PacketChannelEvent<T>(this, data));

            selector.addChannelInterestNow(sc, SelectionKey.OP_READ);

        } catch (IOException ex) {

            closeAndNotifySocketException(ex);

        }
    }

    @Override
    public void handleWrite() {
        // called from selector thread when socket is ready for writing

        try {

            boolean flushedAll;

            synchronized (outBuffer) {
                flushedAll = flushBufferToSocket(outBuffer, sc);
            }

            if (flushedAll) {
                // TODO: this should be done only if in not-ready state if performance suffers
                listener.onSocketReadyForWrite();
            } else {
                listener.onSocketNotReadyForWrite();
                selector.addChannelInterestNow(sc, SelectionKey.OP_WRITE);
            }

        } catch (Exception e) {
            closeAndNotifySocketException(e);
        }
    }

    /**
     * Synchronized to serialize concurrent sendPacket() calls on same PacketChannel.
     *
     * @param packet to send
     * @throws IOException
     */
    public synchronized void sendPacket(T packet) throws IOException {

        boolean isAppendToEmptyOutBuffer = false;

        synchronized (outBuffer) {
            isAppendToEmptyOutBuffer = (outBuffer.position() == 0);
            appendToBuffer(outBuffer, packet.getBytes());
        }

        if (isAppendToEmptyOutBuffer) {
            // don't write to socket here, only request write ready notification
            requestWriteReadyNotificationLater();
        }

        // notify pdu channel listener about sent packet
        listener.packetSent(this, packet);
    }

    protected void appendToBuffer(ByteBuffer buffer, byte[] bytes) throws IOException {

        try {
            buffer.put(bytes);
        } catch (BufferOverflowException ex) {
            throw new IOException("could not append to buffer: total bytes in buffer=" + buffer.position(), ex);
        }
    }

    /**
     * Tries to flush complete outBuffer content to socket channel.<br>
     * <br>
     * {@link #outBuffer} is flipped to prepare it for reading
     *
     * @return
     * @throws IOException when socket channel write fails
     */
    protected boolean flushBufferToSocket(ByteBuffer outBuffer, SocketChannel sc) throws IOException {
        // prepare for getting data from buffer - buffer is ready for reading immediately after this call
        outBuffer.flip();

        // if no data in buffer, we are done (position and limit are zero in this case, because we flip first)
        if (outBuffer.hasRemaining() == false) {
            outBuffer.compact();
            return true;
        }

        // write buffer to socket channel
        sc.write(outBuffer);

        // prepare for putting data in buffer - outBuffer.put() can follow immediately after this call
        // (any bytes not written will be copied to beginning of buffer)
        outBuffer.compact();

        // return true if complete buffer content was flushed to sockect channel
        return outBuffer.position() == 0;
    }

    /**
     * Causes selector thread to register socket channel for OP_WRITE ready notification on next selector process iteration.<br>
     * <br>
     * Cannot register 'now' because this is called from other threads.
     *
     * @see SelectorThread#addChannelInterestLater(java.nio.channels.SelectableChannel, int, CallbackErrorHandler)
     */
    private void requestWriteReadyNotificationLater() {
        selector.addChannelInterestLater(sc, SelectionKey.OP_WRITE, writeReadyRequestCallbackErrorHandler);
    }

    /**
     * Causes selector thread to register socket channel for OP_READ ready notification on next selector process iteration.<br>
     * <br>
     * Cannot register 'now' because this is called from other threads.
     *
     * @see SelectorThread#addChannelInterestLater(java.nio.channels.SelectableChannel, int, CallbackErrorHandler)
     */
    private void requestReadReadyNotificationLater() {
        selector.addChannelInterestLater(sc, SelectionKey.OP_READ, readReadyRequestCallbackErrorHandler);
    }

    private byte[] toByteArray(int readBytes, ByteBuffer buffer) {
        byte[] data = new byte[readBytes];

        buffer.flip();
        buffer.get(data);

        return data;
    }

    /**
     * If socket was not already closed : issues socket channel close request to be executed on selector thread.<br>
     * <br>
     * Executed on selector thread to avoid {@link CancelledKeyException} if selector is processing socket keys at
     * the same time when some other thread closes socket.
     */
    private boolean close() {

        if (!opened.getAndSet(false)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("closing already closed channel: " + sc);
            }
            return false;
        }

        selector.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    sc.close();
                } catch (IOException ex) {
                    LOGGER.error("error closing socket channel: " + sc, ex);
                }
            }
        });

        return true;
    }

    /**
     * Called asynchronously to extract PACKET packets from specified byte[].<br>
     * <br>
     * {@link PacketChannelListener} is notified about each successfully extracted PACKET packet.<br>
     *
     * @param data to extract PACKET from
     */
    public void processData(byte[] data) {
        List<T> packetList = packetAssembler.appendReceivedData(this, data);

        for (T packet : packetList) {
            notifyListenerPacketArrived(packet);
        }
    }

    /**
     * Notifies {@link PacketChannelListener} new PACKET packet is ready.
     *
     * @param packet
     */
    private void notifyListenerPacketArrived(T packet) {
        try {
            listener.packetArrived(this, packet);
        } catch (Exception e) {
            LOGGER.error("error notifying pdu channel listener about new pdu: " + e.getMessage(), e);
        }
    }

    private void closeAndNotifySocketException(Exception ex) {
        if (close()) {
            listener.socketException(this, ex);
        }
    }

    private void closeAndNotifySocketClosed() {
        if (close()) {
            listener.socketDisconnected(this);
        }
    }

    private CallbackErrorHandler createWriteReadyRequestCallbackErrorHandler() {
        return new CallbackErrorHandler() {
            @Override
            public void handleError(Exception ex) {
                LOGGER.error("error requesting write ready notification: sc=" + sc + ", error=" + ex.getMessage());
            }
        };
    }

    private CallbackErrorHandler createReadReadyRequestCallbackErrorHandler() {
        return new CallbackErrorHandler() {
            @Override
            public void handleError(Exception ex) {
                LOGGER.error("error requesting read ready notification: sc=" + sc + ", error=" + ex.getMessage());
            }
        };
    }

    /**
     * Closes socket if socket is not already closed.<br>
     * <br>
     * Does not send notification since we were called from above layer.
     */
    public void stop() {
        close();
    }

    /**
     * Returns the IP socket address to which socket of this {@link PacketChannel} is connected to.
     *
     * @return the IP socket address to which socket of this PacketChannel is connected, or null if the socket is not connected.
     */
    public InetSocketAddress getRemoteSocketAddress() {
        return this.remoteSocketAddress;
    }

    private InetSocketAddress getRemoteSocketAddressFrom(SocketChannel sc) {
        try {

            if (sc == null) {
                return null;
            }

            return new InetSocketAddress(sc.socket().getInetAddress(), sc.socket().getPort());

        } catch (Exception ex) {
            LOGGER.error("error getting remote socket address: " + ex.getMessage());
        }

        return null;
    }

    public InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress) sc.socket().getLocalSocketAddress();
    }
}
