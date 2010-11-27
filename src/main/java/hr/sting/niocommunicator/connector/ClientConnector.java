package hr.sting.niocommunicator.connector;

import hr.sting.niocommunicator.SelectorThread;
import hr.sting.niocommunicator.handler.Connector;
import hr.sting.niocommunicator.handler.ConnectorListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Handles client connection request to NIO server.<br>
 * <br>
 * Notifies {@link ConnectorListener} about
 *
 * @author Marko Stipanov
 */
public class ClientConnector implements ConnectorListener {
    private static final Log LOGGER = LogFactory.getLog(ClientConnector.class);

    private ClientConnectorListener listener;

    private Connector connector;

    public ClientConnector(SelectorThread selector, InetSocketAddress serverAddress, ClientConnectorListener listener, Object context) {
        this.listener = listener;

        this.connector = new Connector(selector, serverAddress, this, context);
    }

    /**
     * Starts client NIO connect.<br>
     * <br>
     * If exception happens while requesting non-blocking connect, reconnect is scheduled after timeout
     */
    public void connect() {

        try {

            connector.connect();

        } catch (Exception ex) {

            connectionFailed(connector, ex);

        }

    }

    @Override
    public void connectionEstablished(Connector connector, SocketChannel sc) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("TCP client connection established: " + connector + ", sc=" + sc + ", starting bind phase...");
        }

        try {

            listener.onTcpClientConnect(this, sc);

        } catch (Exception ex) {

            LOGGER.error("error notifying listener about new tcp client connection, error=" + ex.getMessage() +
                    ", connector=" + connector + ", sc=" + sc, ex);

        }
    }

    @Override
    public void connectionFailed(Connector connector, Exception cause) {
        LOGGER.info("Client connection failed: " + connector +
                ", cause=" + (cause != null ? cause.getMessage() + " (" + cause.getClass().getName() + ")" : "null"));

        try {

            listener.onTcpClientConnectFailed(this, connector.getContext());

        } catch (Exception ex) {

            LOGGER.error("error notifying listener about failed tcp client connection, error=" + ex.getMessage() +
                    ", connector" + connector + ", cause=" + ex.getMessage(), ex);

        }
    }

    public Object getContext() {
        return connector.getContext();
    }
}
