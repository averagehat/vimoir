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
 * A tcp server.
 *
 * Delegate each accepted connection to a new instance of clazz, that must be a
 * subclass of Connection.
 */
class Server extends Dispatcher {
    Class clazz;

    Server(Class clazz, String host, int port) throws java.io.IOException {
        super();
        this.initServer(clazz, host, port);
    }

    /**
     * Constructor.
     *
     * @param selector  Selector used for the Server and all accepted incoming
     *                  connections channels
     * @param clazz     Class object of the incoming connections channels
     * @param host      host name, null to listen from any interface (INADDR_ANY)
     * @param port      port number to listen to
     */
    Server(Selector selector,
                Class clazz, String host, int port) throws java.io.IOException {
        super(selector);
        this.initServer(clazz, host, port);
    }

    void initServer(Class clazz, String host, int port) throws java.io.IOException {
        String ConnectionClass = "vimoir.netbeans.Connection";
        try {
            assert Class.forName(ConnectionClass).isAssignableFrom(clazz)
                                        : "not a subclass of Connection";
        } catch (java.lang.ClassNotFoundException e) {
            logger.severe(e.toString());
            System.exit(1);
        }
        this.clazz = clazz;
        this.createSocket(true);
        this.setReuseAddr();
        this.bind(host, port);
        this.listen();
    }

    void handleRead() {}

    void handleWrite() {}

    void handleTick() {}

    void handleAccept(SocketChannel channel) {
        Connection conn = null;
        try {
            conn = (Connection) this.clazz.newInstance();
        } catch (java.lang.InstantiationException e) {
            logger.severe(e.toString() + this.toString());
        } catch (java.lang.IllegalAccessException e) {
            logger.severe(e.toString() + this.toString());
        }
        if (conn != null) {
            logger.info(this.toString());
            conn.selector = this.selector;
            conn.setSocketChannel(channel);
            // this Server accepts only one connection
            this.close();
        }
    }

    void handleConnect() {}
}
