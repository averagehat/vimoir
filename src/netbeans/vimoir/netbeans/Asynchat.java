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
import java.nio.channels.Selector;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharacterCodingException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A class supporting chat-style (command/response) protocols.
 *
 * This is an abstract class. You must derive from this class, and implement
 * the two methods collect_incoming_data and found_terminator.
 *
 */
abstract class Asynchat extends Dispatcher {
    static final int BUFFER_SIZE = 4096;
    static Charset charset = Charset.forName("US-ASCII");
    static CharsetEncoder encoder = charset.newEncoder();
    static CharsetDecoder decoder = charset.newDecoder();
    ConcurrentLinkedQueue output_queue = new ConcurrentLinkedQueue();
    String terminator = null;
    ByteBuffer outbuf;
    ByteBuffer inbuf;
    int termlen = 0;

    Asynchat() throws IOException {
        super();
        this.initAsynchat();
    }

    /**
     * Constructor.
     *
     * @param selector  Selector used for this channel
     */
    Asynchat(Selector selector) throws IOException {
        super(selector);
        this.initAsynchat();
    }

    void initAsynchat() {
        this.outbuf = ByteBuffer.allocate(BUFFER_SIZE);
        this.outbuf.flip();
        this.inbuf = ByteBuffer.allocate(BUFFER_SIZE);
    }

    /**
     * Set the terminator.
     *
     * @param terminator the terminator
     */
    void setTerminator(String terminator) {
        this.terminator = terminator;
        this.termlen = terminator.length();
    }

    /**
     * This method is invoked when data has been read into <code>str</code>
     * from the channel.
     *
     * @param str   received string
     */
    abstract void collect_incoming_data(String str);

    /**
     * This method is invoked when the terminator has been received from the
     * channel.
     */
    abstract void found_terminator() throws NetbeansException;

    boolean readyToWrite() {
        return (this.state.writable()
                && (this.output_queue.size() != 0 || this.outbuf.remaining() != 0));
    }

    void initiate_send() throws IOException {
        String str = null;
        while ((str = (String) this.output_queue.peek()) != null
                && this.refill_buffer(str))
            output_queue.poll();
        super.send(this.outbuf);
    }

    /** Refill the output buffer.  */
    boolean refill_buffer(String str) {
        int len = str.length();
        if (len == 0) return true;
        if (len > BUFFER_SIZE - this.outbuf.remaining())
            return false;
        if (len > BUFFER_SIZE - this.outbuf.limit())
            this.outbuf.compact().flip();

        this.outbuf.mark().position(this.outbuf.limit()).limit(BUFFER_SIZE);
        try {
            this.outbuf.put(this.encoder.encode(CharBuffer.wrap(str)));
        } catch (CharacterCodingException e) {
            handle_error(e);
            System.exit(1);
        }
        this.outbuf.limit(this.outbuf.position()).reset();
        return true;
    }

    void send(String str) {
        this.output_queue.add(str);
    }

    String recv() {
        try {
            super.recv(this.inbuf);
            this.inbuf.flip();
            String str =  this.decoder.decode(this.inbuf).toString();
            this.inbuf.clear();
            return str;
        } catch (IOException e) {
            logger.severe(e.toString());
            this.handle_close();
            return "";
        }
    }

    void handle_write() {
        try {
            this.initiate_send();
        } catch (IOException e) {
            handle_error(e);
        }
    }
    void handle_read() {
        String str = this.recv();
        if (str == null || str.length() == 0)
            return;
        if (this.terminator == null) {
            this.collect_incoming_data(str);
            return;
        }

        int pos = 0;
        int len = str.length();
        while (pos < len) {
            int index = str.indexOf(this.terminator, pos);
            if (index == -1) {
                this.collect_incoming_data(str.substring(pos));
                break;
            } else {
                // Don't report an empty string.
                if (index > 0)
                    this.collect_incoming_data(str.substring(pos, index));
                pos = index + this.termlen;
                try {
                    this.found_terminator();
                } catch (NetbeansException e) {
                    handle_error(e);
                    System.exit(1);
                }
            }
        }
    }
}
