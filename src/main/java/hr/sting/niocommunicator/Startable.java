package hr.sting.niocommunicator;

/**
 * @author Marko Stipanov
 * @since 08.10.2010. 20:32:21
 */
public interface Startable {
    public void start() throws Exception;

    void stop() throws Exception;
}
