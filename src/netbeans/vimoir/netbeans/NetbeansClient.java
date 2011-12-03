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

public abstract class NetbeansClient implements NetbeansClientType {
    public NetbeansType nbsock;

    public NetbeansClient() {
        this.nbsock = new Netbeans();
    }

    public void start() {
        this.nbsock.start((NetbeansClientType) this);
    }

    //-----------------------------------------------------------------------
    //   Events
    //-----------------------------------------------------------------------

    public void event_open() {}
    public void event_close() {}
    public void event_fileOpened(String pathname) {}
    public void event_killed(String pathname) {}
    public void event_error(String message) {}

    //-----------------------------------------------------------------------
    //   Commands
    //-----------------------------------------------------------------------

    public void default_cmd_processing(String cmd, String args, String pathname, int lnum, int col) {}
}

