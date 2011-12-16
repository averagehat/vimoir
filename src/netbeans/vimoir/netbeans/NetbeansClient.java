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

/**
 * Abstract class that implements the Netbeans commands and functions.
 */
public abstract class NetbeansClient implements NetbeansClientType {
    private NetbeansType nbsock;

    /** Instantiate the Netbeans engine. */
    public NetbeansClient() throws IOException {
        this.nbsock = new Netbeans();
    }

    /**
     * Set the Netbeans engine.
     *
     * @param nbsock the Netbeans engine
     */
    public void setNbsock(NetbeansType nbsock) {
        this.nbsock = nbsock;
    }

    /** Start listening on the Netbeans port and process the Netbeans protocol. */
    public void start() throws IOException {
        this.nbsock.start((NetbeansClientType) this);
    }

    /** Return the Buffer instance associated with this pathname. */
    public Buffer get_buffer(String pathname) throws NetbeansException {
        return this.nbsock.get_buffer(pathname);
    }

    /**
     * Return a quoted string where special characters are escaped.
     *
     * @param text the string to quote
     * @see help in Vim <code>:help nb-terms</code>
     */
    public static String quote(String text) {
        return Netbeans.quote(text);
    }

    /**
     * Send a command to Vim.
     *
     * @param buf the Buffer instance
     * @param cmd the Netbeans command name
     */
    public void send_cmd(Buffer buf, String cmd) {
        this.send_cmd(buf, cmd, "");
    }

    /**
     * Send a command to Vim.
     *
     * @param buf the Buffer instance
     * @param cmd the Netbeans command name
     * @param args the command parameters
     */
    public void send_cmd(Buffer buf, String cmd, String args) {
        this.nbsock.send_request("{0}:{1}!{2}{3}{4}", buf, cmd, args);
    }
}

