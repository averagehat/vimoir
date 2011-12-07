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
import java.util.logging.Logger;

class Netbeans extends Connection implements NetbeansType {
    static final int PORT = 3219;
    private NetbeansClientType client;

    Netbeans() throws IOException {
        super();
        this.setTerminator("\n");
    }

    void foundTerminator() {
        // XXX
        System.out.println(this.getBuff());
    }

    public void start(NetbeansClientType client) throws IOException {
        String className = "vimoir.netbeans.Netbeans";
        try {
            Server s = new Server(Class.forName(className), null, PORT);
        } catch (java.lang.ClassNotFoundException e) {
            logger.severe(e.toString());
            System.exit(1);
        }
        catch (java.net.SocketException e) {
            logger.severe(e.toString());
            System.exit(1);
        }
        Dispatcher.loop();
    }
}

