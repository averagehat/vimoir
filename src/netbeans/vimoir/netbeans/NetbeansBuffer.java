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

import java.io.File;

/**
 * A Vim buffer.
 *
 * <p>Never directly instantiated by the application.
 */
public class NetbeansBuffer {
    /** The full pathname of this buffer. */
    public String pathname;
    /** True when the buffer has been registered with Netbeans. */
    public boolean registered = false;
    /** The cursor position in the buffer as a byte offset. */
    public int offset = 0;
    /** The line number of the cursor (first line is one). */
    public int lnum = 1;
    /** The column number of the cursor (in bytes, zero based). */
    public int col = 0;
    int buf_id;
    Netbeans nbsock;

    NetbeansBuffer(String pathname, int buf_id, Netbeans nbsock) {
        this.pathname = pathname;
        this.buf_id = buf_id;
        this.nbsock = nbsock;
    }

    /** Register the buffer with Netbeans. */
    public void register() {
        this.register(true);
    }

    void register(boolean editFile) {
        // Register file with netbeans.
        if (! this.registered) {
            if (editFile)
                this.nbsock.send_cmd(this, "editFile", this.nbsock.quote(this.pathname));
            this.nbsock.send_cmd(this, "putBufferNumber", this.nbsock.quote(this.pathname));
            this.nbsock.send_cmd(this, "netbeansBuffer", "T");
            this.registered = true;
        }
    }

    /** Return the basename of the buffer pathname. */
    public String get_basename() {
        return new File(this.pathname).getName();
    }

    /** Return the string representation of the buffer. */
    public String toString() {
        return this.get_basename() + ":" + this.lnum + "/" + this.col;
    }
}
