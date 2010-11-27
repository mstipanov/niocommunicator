package hr.sting.niocommunicator.connector;

import hr.sting.niocommunicator.SelectorThread;
import hr.sting.niocommunicator.handler.Acceptor;
import hr.sting.niocommunicator.handler.AcceptorListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class ServerConnector implements AcceptorListener {
    private static final Log LOGGER = LogFactory.getLog(ServerConnector.class);

    private SelectorThread selector;
    private InetSocketAddress serverAddress;
    private ServerConnectorListener listener;

    private Acceptor acceptor;

    public ServerConnector(SelectorThread selector, InetSocketAddress serverAddress, ServerConnectorListener listener) {
        this.selector = selector;
        this.serverAddress = serverAddress;
        this.listener = listener;

        if (this.listener == null) {
            throw new IllegalArgumentException("listener is null");
        }

        if (this.selector == null) {
            throw new IllegalArgumentException("selector is null");
        }
    }

    public void start() throws IOException {

        if (serverAddress == null || serverAddress.getAddress() == null || serverAddress.getPort() <= 0) {
            LOGGER.info("NIO server disabled (server address settings are invalid): " + serverAddress);
            return;
        }

        acceptor = new Acceptor(this.serverAddress, selector, this);

        acceptor.openServerSocket();

        LOGGER.info("NIO server started and listening on: " + serverAddress);
    }

    public void stop() {

        if (acceptor == null) {
            // server not started, just return
            return;
        }

        acceptor.close();

        LOGGER.info("NIO server stopped");
    }

    @Override
    public void socketConnected(Acceptor acceptor, SocketChannel sc) {

        // never let any exception propagate to selector
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Incomming client TCP connection accepted: acceptor=" + acceptor + ", socketChannel=" + sc);
            }

            listener.onTcpServerConnect(this, sc);

        } catch (Exception ex) {

            LOGGER.error("error notifying listener about new tcp server connection, error=" + ex.getMessage() +
                    ", acceptor=" + acceptor + ", sc=" + sc, ex);

        }
    }

    @Override
    public void socketError(Acceptor acceptor, Exception ex) {

        try {
            // just log error - nothing better to do anyway since no server connection was established
            LOGGER.error("acceptor socket error: acceptor=" + acceptor + ", error=" + ex.getMessage(), ex);

        } catch (Exception e) {

            LOGGER.error("error processing socket error, error=" + ex.getMessage() + ", acceptor=" + acceptor, e);
        }

    }
}
