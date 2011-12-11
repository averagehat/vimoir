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
import java.util.Properties;
import java.io.IOException;

class Netbeans extends Connection implements NetbeansType {
    NetbeansClientType client;
    Properties props;

    Netbeans() throws IOException {
        super();
        this.props = new Properties();
        URL url = ClassLoader.getSystemResource("vimoir.properties");
        if (url != null)
            this.props.load(url.openStream());
        this.setTerminator("\n");
    }

    void foundTerminator() {
        // XXX
        System.out.println(this.getBuff());
    }

    public void start(NetbeansClientType client) throws IOException {
        String className = "vimoir.netbeans.Netbeans";
        String host = this.props.getProperty("vimoir.netbeans.host", "");
        if (host == "") host = null;
        try {
            Server s = new Server(Class.forName(className), host,
                                Integer.parseInt(
                                    this.props.getProperty(
                                        "vimoir.netbeans.port", "3219")));
        } catch (java.lang.ClassNotFoundException e) {
            logger.severe(e.toString());
            System.exit(1);
        }
        catch (java.net.SocketException e) {
            logger.severe(e.toString());
            System.exit(1);
        }
        this.client = client;
        Dispatcher.loop();
    }
}

