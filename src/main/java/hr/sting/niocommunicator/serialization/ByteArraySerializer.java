package hr.sting.niocommunicator.serialization;

import java.io.IOException;

/**
 * @author mstipanov
 * @since 24.10.10. 06:57
 */
public interface ByteArraySerializer {
    byte[] serialize(Object o) throws IOException;

    <T> T deserialize(byte[] bytes, Class<T> aClass) throws IOException, ClassNotFoundException;
}
