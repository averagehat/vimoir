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
 * This class implements all the {@link NetbeansEventHandler} methods.
 *
 * <p>This class exists so as to be subclassed by implementations that want to
 * ignore most events and to implement only the ones they are interested in.
 */
public class NetbeansClient implements NetbeansEventHandler {
    /** The Netbeans socket. */
    public NetbeansSocket nbsock;

    public NetbeansClient (NetbeansSocket nbsock) {
        this.nbsock = nbsock;
    }

    //-----------------------------------------------------------------------
    //   Events
    //-----------------------------------------------------------------------

    /** Terminate the server on startup to allow only one Netbeans session. */
    public void event_startupDone() {
        this.nbsock.terminate_server();
    }

    public void event_disconnect() {}

    public void event_fileOpened(NetbeansBuffer buf) {}

    public void event_killed(NetbeansBuffer buf) {}

    public void event_version(String version) {}

    public void event_balloonText(String text) {}

    public void event_buttonRelease(NetbeansBuffer buf, int button) {}

    public void event_keyCommand(NetbeansBuffer buf, String keyName) {}

    public void event_newDotAndMark(NetbeansBuffer buf) {}

    public void event_insert(NetbeansBuffer buf, String text) {}

    public void event_remove(NetbeansBuffer buf, int length) {}

    public void event_save(NetbeansBuffer buf) {}

    public void event_tick() {}

    //-----------------------------------------------------------------------
    //   Commands
    //-----------------------------------------------------------------------

    public void default_cmd_processing(NetbeansBuffer buf, String keyName, String args) {}
}
