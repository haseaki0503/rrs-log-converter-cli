package server;

import java.io.IOException;


/**
 * The Networking Server Interface
 * */
public interface Server {
    void start() throws IOException;
    void shutdown();
    boolean isClosed();
}
