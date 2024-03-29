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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Handle an outgoing or incoming Asynchat connection.
 *
 * This is an abstract class. You must derive from this class, and implement
 * found_terminator().
 *
 */
abstract class Connection extends Asynchat {
    static final int BUFFER_SIZE = 1024;
    SocketAddress remote = null;
    StringBuffer ibuff = new StringBuffer(BUFFER_SIZE);

    Connection() throws IOException {
        super();
    }

    Connection(Selector selector) throws IOException {
        super(selector);
    }

    Connection(String host, int port) throws IOException {
        super();
        this.createSocket(false);
        this.connect(host, port);
    }

    /**
     * Use a constructor whith (host, port) parameters for outgoing
     * connections.
     *
     * @param selector  Selector used for this channel
     * @param host      host name
     * @param port      port number
     */
    Connection(Selector selector,
                    String host, int port) throws IOException {
        super(selector);
        this.createSocket(false);
        this.connect(host, port);
    }

    /**
     * Register the <code>channel</code>. This method is used to register
     * a channel after it has been accepted from a listening socket.
     *
     * @param channel the SocketChannel to register with the selector
     */
    void setSocketChannel(SocketChannel channel) {
        assert channel != null :  "null channel";
        try {
            this.setChannel(channel);
        } catch (IOException e) {
            logger.severe(e.toString());
            this.handle_close();
            return;
        }
        this.remote = channel.socket().getRemoteSocketAddress();
        this.state.connected();
    }

    /**
     * @return the content of the input buffer and clear its content.
     */
    String getBuff() {
        String str = this.ibuff.toString();
        int len = this.ibuff.length();
        this.ibuff.delete(0, len);
        return str;
    }

    void handle_accept(SocketChannel channel) throws IOException {}

    void handle_tick() {}

    void handle_connect() {
        logger.info("connected: " + this.toString());
    }

    void handle_close() {
        logger.info("disconnecting: " + this.toString());
        super.handle_close();
    }

    void collect_incoming_data(String str) {
        this.ibuff.append(str);
    }

    public String toString() {
        if (this.remote == null)
            return super.toString();
        return ("[remote=" + this.remote.toString() + "]");
    }

}
