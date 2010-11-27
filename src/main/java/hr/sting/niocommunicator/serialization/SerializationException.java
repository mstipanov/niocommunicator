package hr.sting.niocommunicator.serialization;

/**
 * Unchecked exception thrown when an error occurs while object (de)serialization {@link
 * java.nio.channels.SocketChannel} using {@link hr.sting.niocommunicator.serialization.ByteArraySerializer} implementations.
 *
 * @author mstipanov
 * @see hr.sting.niocommunicator.serialization.ByteArraySerializer
 * @since 21.04.2010. 10:26:48
 */
public class SerializationException extends RuntimeException {
    public SerializationException(Throwable cause) {
        super(cause);
    }
}