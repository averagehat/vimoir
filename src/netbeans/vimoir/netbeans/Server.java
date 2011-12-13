/*
 * Copyright 2011 Xavier de Gaye
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vimoir.netbeans;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Delegate an accepted connection to a Connection instance.
 */
class Server extends Dispatcher {
    Connection conn;

    Server(Connection conn, String host, int port) throws java.io.IOException {
        super();
        this.initServer(conn, host, port);
    }

    /**
     * Constructor.
     *
     * @param selector  Selector used for the Server and all accepted incoming
     *                  connections channels
     * @param conn      Connection instance
     * @param host      host name, null to listen from any interface (INADDR_ANY)
     * @param port      port number to listen to
     */
    Server(Selector selector, Connection conn,
                String host, int port) throws java.io.IOException {
        super(selector);
        this.initServer(conn, host, port);
    }

    void initServer(Connection conn, String host, int port)
                                                throws java.io.IOException {
        this.conn = conn;
        this.createSocket(true);
        this.setReuseAddr();
        this.bind(host, port);
        this.listen();
    }

    void handleRead() {}

    void handleWrite() {}

    void handleTick() {}

    void handleAccept(SocketChannel channel) {
        logger.info(this.toString());
        this.conn.selector = this.selector;
        this.conn.setSocketChannel(channel);
        // this Server accepts only one connection
        this.close();
    }

    void handleConnect() {}
}
