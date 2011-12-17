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

import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.lang.reflect.Constructor;

/**
 * Delegate an accepted connection to a new Connection.
 */
class Server extends Dispatcher {
    Netbeans nbsock = null;

    Server(String host, int port) throws IOException {
        super();
        this.initServer(host, port);
    }

    /**
     * Constructor.
     *
     * @param selector  Selector used for the Server and all accepted incoming
     *                  connections channels
     * @param host      host name, null to listen from any interface (INADDR_ANY)
     * @param port      port number to listen to
     */
    Server(Selector selector, String host, int port) throws IOException {
        super(selector);
        this.initServer(host, port);
    }

    void initServer(String host, int port) throws IOException {
        this.createSocket(true);
        this.setReuseAddr();
        this.bind(host, port);
        this.listen();
    }

    void handle_read() {}

    void handle_write() {}

    void handle_tick() {}

    void handle_accept(SocketChannel channel) throws IOException {
        if (this.nbsock != null && this.nbsock.state.is_connected()) {
            channel.close();
            logger.info("rejecting connection from '"
                        + channel + "' netbeans already connected");
            return;
        }

        // get the class to instantiate
        Properties props = new Properties();
        URL url = ClassLoader.getSystemResource("vimoir.properties");
        if (url != null) {
            InputStream f = url.openStream();
            props.load(f);
            f.close();
        }
        String name = props.getProperty("vimoir.netbeans.java.client", "vimoir.examples.Phonemic");
        this.nbsock = new Netbeans(this, props);
        NetbeansEventHandler client = null;
        try {
            Class clazz = Class.forName(name);
            Class[] types = { vimoir.netbeans.NetbeansEngine.class };
            Constructor constructor = clazz.getConstructor(types);
            Object[] params = { this.nbsock };
            client = (NetbeansEventHandler) constructor.newInstance(params);
        } catch (Exception e) {
            logger.severe(e.toString());
            System.exit(1);
        }

        this.nbsock.set_client(client);
        this.nbsock.selector = this.selector;
        this.nbsock.setSocketChannel(channel);
        logger.info("accepting: " + channel);
    }

    void handle_connect() {}
}
