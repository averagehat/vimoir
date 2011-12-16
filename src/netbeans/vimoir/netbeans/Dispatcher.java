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

import java.util.Date;
import java.util.Iterator;
import java.util.logging.Logger;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.net.URL;
import java.util.Properties;
import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ClosedChannelException;

/**
 * Implementation of the python asyncore design pattern.
 *
 * The basic idea behind the asyncore design is that you must instantiate the
 * initial channels from subclasses of Dispatcher, then register them with a
 * Selector, and start the Dispatcher loop.
 *
 * Registering a channel to a Selector is done by invoking  the createSocket
 * method, or by using a constructor that takes a SocketChannel as parameter.
 * The class default_selector is used as the Selector by default.
 *
 * Each channel gets IO events through the handle_read, handle_write,
 * handle_accept and handle_connect methods. Note that all events are not always
 * meaningful depending on the socket type, for example outgoing connections
 * never receive handle_accept events.
 *
 * All channels receive timer events through the handle_tick method, and it is
 * possible to instantiate a channel that receives only timer events by
 * subclassing the Timer class.
 *
 * As a workaround against a bug that occurs with java.nio on linux, a
 * Dispatcher instance should always have at least one of its 'readyTo' methods
 * return True. See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
 *
 */
abstract class Dispatcher {
    static Logger logger = Logger.getLogger("vimoir.netbeans");
    static Selector default_selector = null;

    Selector selector;
    ConnectionState state;
    SelectableChannel channel;
    InetSocketAddress address;

    Dispatcher() throws IOException {
        this.initDispatcher(null, null);
    }

    Dispatcher(Selector selector) throws IOException {
        this.initDispatcher(selector, null);
    }

    Dispatcher(SocketChannel channel) throws IOException {
        assert channel != null :  "null channel";
        this.initDispatcher(null, channel);
        this.setChannel(channel);
        this.state.connected();
    }

    /**
     * Use this constructor when the SocketChannel parameter has been obtained
     * from an invocation of handle_accept by another listening channel.
     *
     * @param selector  Selector used for this channel
     * @param channel   associated SocketChannel instance
     */
    Dispatcher(Selector selector, SocketChannel channel) throws IOException {
        assert channel != null :  "null channel";
        this.initDispatcher(selector, channel);
        this.setChannel(channel);
        this.state.connected();
    }

    void initDispatcher(Selector selector, SelectableChannel channel)
                                                throws IOException {
        if (selector == null) {
            if (default_selector == null)
                default_selector = Selector.open();
            selector = default_selector;
        }
        this.selector = selector;
        this.state = new ConnectionState();
        this.channel = channel;
        this.address = null;
    }

    /**
     * This method is invoked whenever readyToRead() is true, and there is data
     * ready to be read from the channel.
     */
    abstract void handle_read();

    /**
     * This method is invoked whenever readyToWrite() is true, and the channel
     * can be written to (a socket can be written to when it is connected and
     * when the internal kernel buffer is not full).
     */
    abstract void handle_write();

    /**
     * This method is invoked whenever readyToAccept() is true, and there is an
     * incoming connection request.
     *
     * The channel passed as a parameter to the method is associated with the
     * incoming connection request. To accept the connection, create a new
     * instance of Dispatcher or one of its subclasses and invoke its
     * setSocketChannel method. See how this is done in class Server.
     *
     * @param channel of the incoming connection request
     */
    abstract void handle_accept(SocketChannel channel);

    /**
     * This method is invoked when readyToConnect() is true, the channel is
     * associated with an outgoing connection and the socket three-way
     * connection handshake has been successfully completed.
     */
    abstract void handle_connect();

    /**
     * This method is invoked at each timer event.
     */
    abstract void handle_tick();

    /**
     * This method is invoked when the channel is about to close. It is invoked
     * after a remote disconnection or on exceptionnal events such as
     * IOException exceptions. A subclass of Dispatcher that overrides this
     * method must call super.handle_close();
     */
    void handle_close() {
        this.close();
    }

    /* One must override the readyToWrite() method to restrict the condition to
     * having data ready to be written. Failing to do so will cause the loop to
     * spin continuously reporting that the channel is ready to be writtent to,
     * and consume a lot of cpu. See how this is done in class Asynchat.
     */
    boolean readyToWrite() { return this.state.writable(); }
    boolean readyToRead() { return this.state.readable(); }
    boolean readyToAccept() { return this.state.acceptable(); }
    boolean readyToConnect() { return this.state.connectable(); }

    /* Return the associated SocketChannel instance. */
    SocketChannel getSocketChannel() {
        assert this.channel != null :  "null channel";
        assert this.channel instanceof SocketChannel : "not a socket";
        return (SocketChannel) this.channel;
    }

    /* Return the associated ServerSocketChannel instance. */
    ServerSocketChannel getServerSocketChannel() {
        assert this.channel != null :  "null channel";
        assert this.channel instanceof ServerSocketChannel : "not a server";
        return (ServerSocketChannel) this.channel;
    }

    /**
     * @return the InetSocketAddress of the socket
     */
    InetSocketAddress getInetSocketAddress() {
        return this.address;
    }

    /**
     * @return true when the socket is connected
     */
    boolean connected() {
        return this.state.writable();
    }

    void addChannel() throws ClosedChannelException {
        assert this.channel != null :  "null channel";
        logger.info("addChannel" + this.toString());
        this.channel.register(this.selector, 0, this);
    }

    void delChannel() {
        if (this.channel != null) {
            SelectionKey key = this.channel.keyFor(this.selector);
            if (key != null)
                key.cancel();
            this.channel = null;
        }
    }

    /**
     * Register the channel with its associated Selector.
     *
     * @param server true when the channel must be associated with a listening
     *               ServerSocketChannel, false when it must be associated
     *               with an outgoing SocketChannel
     */ 
    void createSocket(boolean server) throws IOException {
        if (server) {
            this.channel = (SelectableChannel) ServerSocketChannel.open();
        } else {
            this.channel = (SelectableChannel) SocketChannel.open();
        }
        this.setChannel(channel);
    }

    /**
     * Call this method with the SocketChannel obtained through a handle_accept
     * event as its parameter, in order to register the channel with
     * its selector.
     *
     * @param channel the SelectableChannel to register with the selector
     */
    void setChannel(SelectableChannel channel)
                            throws IOException, ClosedChannelException {
        this.channel = channel;
        this.channel.configureBlocking(false);
        this.addChannel();
    }

    /**
     * Set the SO_REUSEADDR socket option that controls whether `bind' should
     * permit reuse of local addresses for this socket.
     */
    void setReuseAddr(){
        assert this.channel != null :  "null channel";
        if (this.channel instanceof ServerSocketChannel) {
            ServerSocketChannel server = (ServerSocketChannel) this.channel;
            try {
                server.socket().setReuseAddress(true);
            } catch (java.net.SocketException e){ /* ignore */ }
        }
    }

    /**
     *  Listen for connections made to the socket.
     */
    void listen() { this.state.accepting(); }

    /**
     * Bind the socket to host, port.
     *
     * @param host  host name
     * @param port  port number
     */
    void bind(String host, int port) throws IOException {
        ServerSocket socket = this.getServerSocketChannel().socket();
        if (host != null)
            this.address = new InetSocketAddress(host, port);
        else
            this.address = new InetSocketAddress(port);
        socket.bind(this.address);
    }

    /**
     * Connect to host, port.
     *
     * @param host  host name
     * @param port  port number
     */
    void connect(String host, int port) throws IOException {
        this.state.connecting();
        this.address = new InetSocketAddress(host, port);
        this.getSocketChannel().connect(this.address);
    }

    /**
     * Write the content of <code>data</code> to the socket.
     *
     * @param data  buffer holding the bytes to be written
     */
    void send(ByteBuffer data) throws IOException {
        this.getSocketChannel().write(data);
    }

    /**
     * Read from the socket into <code>data</code>.
     *
     * @param data  buffer holding the bytes that have been read
     */
    int recv(ByteBuffer data) throws IOException {
        int count = this.getSocketChannel().read(data);
        /* a closed connection is indicated by signaling
         * a read condition, and having read() return -1. */
        if (count == -1) {
            this.handle_close();
            count = 0;
        }
        return count;
    }

    /**
     * Close the socket and remove the channnel from its selector.
     */
    void close() {
        logger.info("close: " + this.toString());
        this.state.closing();
        if (this.channel != null) {
            try {
                if (this.channel instanceof ServerSocketChannel)
                    ((ServerSocketChannel) this.channel).socket().close();
                else if (this.channel instanceof SocketChannel)
                    ((SocketChannel) this.channel).socket().close();
            } catch (IOException e) { /* ignore */ }
        }
        this.delChannel();
    }

    void handle_read_event() {
        //logger.finest("handle_read_event" + this.toString());
        this.handle_read();
    }

    void handle_write_event() {
        //logger.finest("handle_write_event" + this.toString());
        this.handle_write();
    }

    void handle_accept_event() {
        SocketChannel channel = null;
        try {
            channel = this.getServerSocketChannel().accept();
        } catch (IOException e) {
            logger.severe(e.toString());
            this.handle_close();
            return;
        }

        if (channel != null) {
            logger.info("handle_accept_event: " + this.toString());
            this.handle_accept(channel);
        }
        else
            logger.severe("handle_accept_event null event: " + this.toString());
    }

    void handle_connect_event() {
        if (! this.connected()) {
            try {
                this.getSocketChannel().finishConnect();
            } catch (IOException e) {
                logger.severe(e.toString());
                this.handle_close();
                return;
            }
            this.state.connected();
            logger.info("handle_connect_event" + this.toString());
            this.handle_connect();
        }
    }

    void handle_tick_event() { this.handle_tick(); }

    public String toString() {
        if (this.channel == null)
            return super.toString();
        else
            return this.channel.toString();
    }

    static void loop() {
        Properties props;
        props = new Properties();
        URL url = ClassLoader.getSystemResource("vimoir.properties");
        try {
            if (url != null) props.load(url.openStream());
        } catch (IOException e) {}
        loop(Long.parseLong(props.getProperty(
                            "vimoir.netbeans.user_interval", "200")));
    }

    static void loop(long user_interval) {
        try {
            if (default_selector == null)
                default_selector = Selector.open();
        } catch (IOException e) {
            logger.severe(e.toString());
            return;
        }
        loop(default_selector, user_interval);
    }

    /**
     *  Enter a polling loop that terminates when all open channels have been
     *  closed.
     *
     *  The selector is a map whose items are the channels to watch.  As
     *  channels are closed they are deleted from their map.
     *
     * @param selector      Selector used for this loop
     * @param user_interval the timer events period in milliseconds
     */
    static void loop(Selector selector, long user_interval) {
        assert selector != null :  "null selector";

        Properties props;
        props = new Properties();
        URL url = ClassLoader.getSystemResource("vimoir.properties");
        try {
            if (url != null) props.load(url.openStream());
        } catch (IOException e) {}
        long timeout = Long.parseLong(props.getProperty(
                                    "vimoir.netbeans.timeout", "20"));

        int bugCount = 0;
        boolean handleJavaFourSelectBug = isJavaFour();
        if (handleJavaFourSelectBug)
            logger.info("Handling select bug in java 1.4");

        Date lastTime = new Date();
        while (! selector.keys().isEmpty()) {

            setSelectionKeys(selector);
            int eventCount = 0;
            try {
                eventCount = selector.select(timeout);
            } catch (IOException e) {
                logger.severe(e.toString());
                return;
            }

            /* Iterate over the resulting selected SelectionKeys and send the
             * corresponding IO events. */
            Iterator it = selector.selectedKeys().iterator();
            SelectionKey key = null;
            while (it.hasNext()) {
                try {
                    key = (SelectionKey) it.next();
                } catch (java.util.ConcurrentModificationException e) {
                    logger.severe("possibly skipping events after a close");
                    it.remove();
                    continue;
                }
                it.remove();

                Dispatcher dispatcher = (Dispatcher) key.attachment();
                if (key.isReadable())
                    dispatcher.handle_read_event();
                if (key.isValid() && key.isWritable())
                    dispatcher.handle_write_event();
                if (key.isValid() && key.isAcceptable())
                    dispatcher.handle_accept_event();
                if (key.isValid() && key.isConnectable())
                    dispatcher.handle_connect_event();
            }

            /* Send the timer events. */
            Date now = new Date();
            if (now.getTime() - lastTime.getTime() >= user_interval) {
                bugCount = 0; // reset the bug counter on a timer event
                lastTime = now;
                it = selector.keys().iterator();
                while (it.hasNext()) {
                    key = (SelectionKey) it.next();
                    if (! key.isValid()) continue;
                    Dispatcher dispatcher = (Dispatcher) key.attachment();
                    dispatcher.handle_tick_event();
                }
            }

            /* java 1.4.2_12 does NOT ALWAYS respect the timeout when there are
             * no events. The following sleep() prevents looping and prevents
             * using all the cpu, in these cases. */
            if (handleJavaFourSelectBug && eventCount == 0) {
                bugCount++;
                if (bugCount > 5) {
                    bugCount = 0;
                    try {
                        Thread.sleep(timeout);
                    } catch (java.lang.InterruptedException e) { /* ignore */ }
                }
            }
        }
    }

    /* Set the SelectionKeys before the call to select. */
    static int setSelectionKeys(Selector selector) {
        int count = 0;
        Iterator it = selector.keys().iterator();
        while (it.hasNext()) {
            int ops = 0;
            SelectionKey key = (SelectionKey) it.next();
            if (! key.isValid()) continue;
            Dispatcher dispatcher = (Dispatcher) key.attachment();
            if (dispatcher.readyToRead()) {
                count++;
                ops |= SelectionKey.OP_READ;
            }
            if (dispatcher.readyToWrite()) {
                count++;
                ops |= SelectionKey.OP_WRITE;
            }
            if (dispatcher.readyToAccept()) {
                count++;
                ops |= SelectionKey.OP_ACCEPT;
            }
            if (dispatcher.readyToConnect()) {
                count++;
                ops |= SelectionKey.OP_CONNECT;
            }
            key.interestOps(ops);
        }
        return count;
    }

    /* Return true when the JVM is 1.4. */
    static boolean isJavaFour() {
        String version = System.getProperty("java.version");
        String[] array = version.split("[^a-zA-Z0-9]+");
        int revision = 0;
        try {
            if (array.length >= 2)
                revision = Integer.parseInt(array[1]);
        } catch(java.lang.NumberFormatException e) { /* ignore */ }
        return revision == 4;
    }
}

class ConnectionState {
    static final int NONE = 0;
    static final int ACCEPTING = 1;
    static final int CONNECTING = 2;
    static final int CONNECTED = 3;
    static final int CLOSING = 4;
    int state = NONE;

    boolean acceptable() { return (this.state == ACCEPTING); }
    boolean connectable() { return (this.state == CONNECTING); }
    boolean readable() { return (this.state == CONNECTED); }
    boolean writable() { return (this.state == CONNECTED); }
    boolean closed() { return (this.state == CLOSING); }

    void accepting() { this.state = ACCEPTING; }
    void connecting() { this.state = CONNECTING; }
    void connected() { this.state = CONNECTED; }
    void closing() { this.state = CLOSING; }

    public String toString() {
        String str = "state: ";
        switch(this.state) {
            case ACCEPTING:
                str += "accepting";
            case CONNECTING:
                str += "connecting";
            case CONNECTED:
                str += "connected";
            case CLOSING:
                str += "closed";
            default:
                str += "none";
        }
        return str;
    }
}
