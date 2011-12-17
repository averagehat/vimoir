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
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.MessageFormat;
import java.lang.reflect.Method;

class Netbeans extends Connection implements NetbeansType {
    private static Pattern re_auth;
    private static Pattern re_event;
    private static Pattern re_response;
    private static Pattern re_lnumcol;
    private static Pattern re_escape;
    private static Pattern re_unescape;
    Server server;
    Properties props;
    NetbeansClientType client = null;
    BufferSet bset;
    boolean ready = false;
    int seqno = 0;
    int last_seqno = 0;

    static {
        re_auth = Pattern.compile("^\\s*AUTH\\s*(\\S+)\\s*$");
        re_event = Pattern.compile("^\\s*(\\d+):(\\S+)=(\\d+)\\s*(.*)\\s*$");
        re_response = Pattern.compile("^\\s*(\\d+)\\s*(.*)\\s*$");
        re_lnumcol = Pattern.compile("^(\\d+)/(\\d+)");
        re_escape = Pattern.compile("[\"\\n\\t\\r\\\\]");
        re_unescape = Pattern.compile("\\\\[\"ntr\\\\]");
    }

    Netbeans(Server server, Properties props) throws IOException {
        super();
        this.server = server;
        this.props = props;
        this.bset = new BufferSet(this);
        this.setTerminator("\n");

        // set the encoding
        Charset charset = Charset.forName(
                props.getProperty("vimoir.netbeans.encoding", "UTF-8"));
        this.encoder = charset.newEncoder();
        this.decoder = charset.newDecoder();
    }

    void set_client(NetbeansClientType client) {
        this.client = client;
    }

    /** Terminate the server. */
    public void terminate_server() {
        logger.info("terminating the server");
        this.server.close();
    }

    /** Process new line terminated netbeans message. */
    void found_terminator() {
        String msg = this.getBuff();
        logger.finest(msg);

        if (! this.ready) {
            try {
                this.open_session(msg);
            } catch (NetbeansException e) {
                System.out.println(e);
                System.exit(1);
            }
            return;
        }

        Parser parsed = null;
        try {
            parsed = new Parser(msg);
        } catch (NetbeansException e) {
            return; // ignore invalid message
        }
        if (parsed.is_event) {
            Class[] parameterTypes = { parsed.getClass() };
            Object[] args = { parsed };
            String event = "evt_" + parsed.event;
            try {
                Method method = this.getClass().getDeclaredMethod(
                                                event, parameterTypes);
                method.invoke((Object) this, args);
            } catch (NoSuchMethodException e) {
                // silently ignore unhandled events
            } catch (Exception e) {
                logger.severe(e.toString());
            }
        } else {
            // A function reply: process the reply.
            // FIXME
        }
    }

    /** Process initial netbeans messages. */
    void open_session(String msg) throws NetbeansException {
        // 'AUTH changeme'
        Matcher matcher = re_auth.matcher(msg);
        if(matcher.matches()) {
            String password = matcher.group(1);
            String expected = this.props.getProperty("vimoir.netbeans.password", "changeme");
            if (! expected.equals(password))
                throw new NetbeansException("invalid password: " + password);
            return;
        // '0:version=0 "2.3"'
        // '0:startupDone=0'
        } else {
            Parser parsed = new Parser(msg);
            if (parsed.is_event) {
                if (parsed.event.equals("version"))
                    return;
                else if (parsed.event.equals("startupDone")) {
                    this.ready = true;
                    this.client.event_startupDone();
                    return;
                }
            }
        }
        throw new NetbeansException("received unexpected message: '" + msg + "'");
    }

    public Buffer get_buffer(String pathname) throws NetbeansException {
        return this.bset.get(pathname);
    }

    //-----------------------------------------------------------------------
    //  Events
    //-----------------------------------------------------------------------

    /** Process a disconnect netbeans event. */
    void evt_disconnect(Parser parsed) {
        this.client.event_disconnect();
        this.close();
    }

    /** A file was opened by the user. */
    void evt_fileOpened(Parser parsed) {
        String pathname = parsed.nbstring;
        if (! pathname.equals("")) {
            Buffer buf = null;
            try {
                buf = this.bset.get(pathname);
            } catch (NetbeansException e) {
                logger.severe("absolute pathname required, got: " + pathname);
                return;
            }
            if (buf.buf_id != parsed.buf_id) {
                if (parsed.buf_id == 0) {
                    buf.register(false);
                    this.client.event_fileOpened(buf);
                }
                else
                    logger.severe("got fileOpened with wrong bufId");
            }
        }
        else
            this.client.event_error(
                "You cannot use netbeans on a '[No Name]' file.\n"
                + "Please, edit a file.");
    }

    /** Process a keyAtPos netbeans event. */
    void evt_keyAtPos(Parser parsed) {
        Buffer buf = this.bset.getbuf_at(parsed.buf_id);
        if (buf == null)
            logger.severe("invalid bufId: " + parsed.buf_id + " in keyAtPos");
        else if (parsed.nbstring.equals(""))
            logger.finest("empty string in keyAtPos");
        else if (parsed.arg_list.length != 2)
            logger.severe("invalid arg in keyAtPos");
        else {
            Matcher matcher = re_lnumcol.matcher(parsed.arg_list[1]);
            if(! matcher.matches()) {
                logger.severe("invalid lnum/col: " + parsed.arg_list[1]);
            } else {
                buf.lnum = Integer.parseInt((matcher.group(1)));
                buf.col = Integer.parseInt((matcher.group(2)));

                String[] splitted = parsed.nbstring.trim().split("\\s+", 2);
                int len = splitted.length;
                String args = "";
                if (len == 2)
                    args = splitted[1];
                Class[] parameterTypes = { args.getClass(), buf.getClass() };
                Object[] parameters = { args, buf };
                String cmd = "cmd_" + splitted[0];
                try {
                    Method method = this.client.getClass().getDeclaredMethod(
                                                    cmd, parameterTypes);
                    method.invoke((Object) this.client, parameters);
                } catch (NoSuchMethodException e) {
                    this.client.default_cmd_processing(splitted[0], args, buf);
                } catch (Exception e) {
                    logger.severe(e.toString());
                }
            }
        }
    }

    /** A file was deleted or wiped out by the user. */
    void evt_killed(Parser parsed) {
        Buffer buf = this.bset.getbuf_at(parsed.buf_id);
        if (buf == null)
            logger.severe("invalid bufId: '" + parsed.buf_id + "' in killed");
        else {
            buf.registered = false;
            this.client.event_killed(buf);
        }
    }

    void handle_tick() {
        this.client.event_tick();
    }

    //-----------------------------------------------------------------------
    //   Commands - Functions
    //-----------------------------------------------------------------------

    /** Send a command to Vim. */
    void send_cmd(Buffer buf, String cmd) {
        this.send_cmd(buf, cmd, "");
    }

    public void send_cmd(Buffer buf, String cmd, String args) {
        this.send_request("{0}:{1}!{2}{3}{4}", buf, cmd, args);
    }

    /** Send a function call to Vim. */
    void send_function(Buffer buf, String function) {
        this.send_function(buf, function, "");
    }

    void send_function(Buffer buf, String function, String args) {
        // FIXME
        this.send_request("{0}:{1}/{2}{3}{4}", buf, function, args);
    }

    /** Send a netbeans function or command. */
    void send_request(String fmt, Buffer buf, String request, String args) {
        this.seqno += 1;
        int buf_id = 0;
        if (buf != null)
            buf_id = buf.buf_id;
        String space = " ";
        if (args.equals(""))
            space = "";
        Object[] prm = {new Long(buf_id), request, new Long(this.seqno), space, args};
        String msg = MessageFormat.format(fmt, prm);

        if (this.state.is_connected()) {
            this.send(msg + '\n');
            logger.finest(msg);
        }
        else
            logger.info("failed to send_request: not connected");
    }

    /** Escape special characters in string.*/
    static String escape_char(char c) {
        if (c == '"') return  "\\\"";
        if (c == '\n') return "\\n";
        if (c == '\t') return "\\t";
        if (c == '\r') return "\\r";
        if (c == '\\') return "\\\\";
        assert false: "regex and escape_char don't match";
        return "";
    }

    /** Quote 'msg' and escape special characters. */
    public String quote(String text) {
        String result = "";
        int i = 0;
        Matcher matcher = re_escape.matcher(text);
        if (matcher.find(0)) {
            do {
                int j = matcher.start();
                if (j != 0)
                    result += text.substring(i, j);
                result += escape_char(text.charAt(j));
                i = j + 1;
            } while (matcher.find());
        }
        result += text.substring(i);
        return "\"" + result + "\"";
    }

    /** Remove escape on special characters in quoted string. */
    static String unescape_char(String escaped) {
        if (escaped == "\\\"") return  "\"";
        if (escaped == "\\n") return "\n";
        if (escaped == "\\t") return "\t";
        if (escaped == "\\r") return "\r";
        if (escaped == "\\\\") return "\\";
        assert false: "regex and unescape_char don't match";
        return "";
    }

    /** Remove escapes from escaped characters in a quoted string. */
    static String unquote(String text) {
        String result = "";
        int i = 0;
        Matcher matcher = re_unescape.matcher(text);
        if (matcher.find(0)) {
            do {
                int j = matcher.start();
                if (j != 0)
                    result += text.substring(i, j);
                result += unescape_char(text.substring(j, j+2));
                i = j + 2;
            } while (matcher.find());
        }
        result += text.substring(i);
        return result;
    }

    /**
     * Start the Netbeans engine.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        URL url = ClassLoader.getSystemResource("vimoir.properties");
        if (url != null) {
            InputStream f = url.openStream();
            props.load(f);
            f.close();
        }

        String host = props.getProperty("vimoir.netbeans.host", "");
        if (host == "")
            host = null;
        new Server(host, Integer.parseInt(props.getProperty(
                                    "vimoir.netbeans.port", "3219")));
        long user_interval = Long.parseLong(props.getProperty(
                                    "vimoir.netbeans.user_interval", "200"));
        long timeout = Long.parseLong(props.getProperty(
                                    "vimoir.netbeans.timeout", "20"));
        Dispatcher.loop(user_interval, timeout);

        // Terminate all Phonemic threads by exiting.
        logger.info("Terminated.");
        System.exit(0);
    }

    /** Parse a received netbeans message. */
    class Parser {
        boolean is_event = false;
        int buf_id = 0;
        String event = "";
        int  seqno = 0;
        String nbstring = "";
        String[] arg_list = {};

        Parser(String msg) throws NetbeansException {
            String args = "";
            Matcher matcher = re_event.matcher(msg);
            if(matcher.matches()) {
                this.is_event = true;
                this.buf_id = Integer.parseInt(matcher.group(1));
                this.event = matcher.group(2);
                this.seqno = Integer.parseInt(matcher.group(3));
                args = matcher.group(4);
            } else {
                matcher = re_response.matcher(msg);
                if(matcher.matches()) {
                    this.seqno = Integer.parseInt(matcher.group(1));
                    args = matcher.group(2);
                }
                else {
                    logger.severe("discarding invalid netbeans message: " + msg);
                    return;
                }
            }

            // a netbeans string
            int end = -1;
            if (args.length() != 0 && args.charAt(0) == '"') {
                end = args.lastIndexOf("\"");
                if (end != -1 && end != 0) {
                    this.nbstring = args.substring(1, end);
                    // Do not unquote nbkey parameter twice since vim already
                    // parses function parameters as strings (see :help
                    // expr-quote).
                    if (! this.event.equals("keyAtPos")) {
                        this.nbstring = unquote(this.nbstring);
                    }
                }
                else
                    end = -1;
            }
            this.arg_list = args.substring(end+1).trim().split("\\s+");
        }
    }

    /**
     * A container for a list and map of buffers.
     *
     * <p> A Buffer instance is never removed from BufferSet.
     */
    class BufferSet {
        Netbeans nbsock;
        ArrayList buf_list = new ArrayList();
        HashMap dict = new HashMap();

        BufferSet(Netbeans nbsock) {
            this.nbsock = nbsock;
        }

        /** Return the Buffer at index buf_id in the list. */
        Buffer getbuf_at(int buf_id) {
            if (buf_id <= 0 || buf_id > this.buf_list.size())
                return null;
            return (Buffer) this.buf_list.get(buf_id - 1);
        }

        /** Get Buffer with pathname as key, instantiate one when not found. */
        Buffer get(String pathname) throws NetbeansException {
            File f = new File(pathname);
            String path = f.getPath();
            String fullpath = f.getAbsolutePath();
            if (! fullpath.equals(path))
                throw new NetbeansException("'" + path + "' is not an absolute"
                                    + " path, do you mean '" + fullpath + "' ?");

            if (! this.dict.containsKey(pathname)) {
                Buffer buf = new Buffer(pathname,
                                this.buf_list.size() + 1, this.nbsock);
                this.buf_list.add(buf);
                this.dict.put(pathname, buf);
            }
            return (Buffer) this.dict.get(pathname);
        }
    }
}

