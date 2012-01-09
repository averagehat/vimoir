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

import java.util.Observer;

/** Interface implemented by the Netbeans socket. */
public interface NetbeansSocket {

    /**
     * Terminate the server.
     *
     * <p> Stop accepting incoming connections. This does not close the current
     * socket connections.
     */
    public void terminate_server();

    /** Close the socket. */
    public void close();

    /**
     * Return the buffer instance associated with this pathname.
     *
     *
     * <p> The NetbeansSocket object maintains a map (dictionary, HashMap) of
     * {pathname: buffer}. When <code>pathname</code> is not in the map, the
     * NetbeansSocket object instantiates a new NetbeansBuffer object and adds
     * this new entry to the map.
     *
     * @param pathname the full pathname of this buffer
     */
    public NetbeansBuffer get_buffer(String pathname) throws NetbeansInvalidPathnameException;

    /**
     * Quote a string and escape special characters.
     *
     * @param text the string to quote
     * @return the quoted string where special characters have been escaped
     */
    public String quote(String text);

    /**
     * Split a string including quoted parts.
     *
     * <p> For example, splitting the string:
     * <pre>
     *      "\"a c\" b v \"this \\"is\\" foobar argument\" Y"
     * </pre>
     * returns the String array:
     * <pre>
     *      { "a c", "b", "v", "this \"is\" foobar argument", "Y" }
     * </pre>
     *
     * @param  text the string to split
     * @return the list of whitespace separated tokens from <code>text</code>,
     * handling double quoted substrings as a token. The '\' escaping character
     * of the special characters in quoted substrings are removed.
     */
    public String[] split_quoted_string(String text);

    /**
     * Send a netbeans command.
     *
     * @param buf   null when the bufID is zero
     * @param cmd   the command name
     */
    public void send_cmd(NetbeansBuffer buf, String cmd);

    /**
     * Send a netbeans command.
     *
     * @param buf   null when the bufID is zero
     * @param cmd   the command name
     * @param args  the command parameters, use the quote method to quote
     *              a Netbeans <code>string</code> parameter
     */
    public void send_cmd(NetbeansBuffer buf, String cmd, String args);

    /**
     * Send a netbeans function.
     *
     * <p> The <code>observer</code> gets the Netbeans asynchronous
     * <code>Reply</code> through the <code>arg</code> parameter ot its {@link
     * java.util.Observer#update} method, which is a (possibly empty) String
     * array. Note that when the <code>Reply</code> contains a Netbeans
     * <code>string</code>, such as with the <code>getText</code> Netbeans
     * function, then the string has been already unquoted by vimoir.
     *
     * <p> See an example with the {@link vimoir.examples.Phonemic#cmd_length}
     * method.
     *
     * @param buf      null when the bufID is zero
     * @param function the function name
     * @param observer the function Observer
     */
    public void send_function(NetbeansBuffer buf, String function, Observer observer);

    /**
     * Send a netbeans function.
     *
     * <p> The <code>observer</code> gets the Netbeans asynchronous
     * <code>Reply</code> through the <code>arg</code> parameter ot its {@link
     * java.util.Observer#update} method, which is a (possibly empty) String
     * array. Note that when the <code>Reply</code> contains a Netbeans
     * <code>string</code>, such as with the <code>getText</code> Netbeans
     * function, then the string has been already unquoted by vimoir.
     *
     * <p> See an example with the {@link vimoir.examples.Phonemic#cmd_length}
     * method.
     *
     * @param buf      null when the bufID is zero
     * @param function the function name
     * @param args     the function parameters, use the quote method to quote
     *                 a Netbeans <code>string</code> parameter
     * @param observer the function Observer
     */
    public void send_function(NetbeansBuffer buf, String function, String args, Observer observer);
}
