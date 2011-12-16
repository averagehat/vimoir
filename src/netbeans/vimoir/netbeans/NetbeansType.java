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

/**
 * Interface implemented by Netbeans engines.
 *
 * <p> There is a java Netbeans engine and a jython Netbeans engine which is
 * implemented by the <code>vimoir.jynetbeans</code> package.
 */
public interface NetbeansType {
    /**
     * Start listening on the Netbeans port and process the Netbeans protocol.
     *
     * @param client object implementing the Netbeans application
     */
    public void start(NetbeansClientType client) throws java.io.IOException;

    /** Return the Buffer instance associated with this pathname. */
    public Buffer get_buffer(String pathname) throws NetbeansException;

    /** Send a netbeans function or command. */
    public void send_request(String fmt, Buffer buf, String request, String args);
}
