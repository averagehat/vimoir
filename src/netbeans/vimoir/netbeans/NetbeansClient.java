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
 * Abstract class that implements the Netbeans commands and functions.
 */
public abstract class NetbeansClient implements NetbeansClientType {
    private NetbeansType nbsock;

    /**
     * Instantiate the Netbeans engine.
     */
    public NetbeansClient() {
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

    /**
     * Start listening on the Netbeans port and process the Netbeans protocol.
     */
    public void start() {
        this.nbsock.start((NetbeansClientType) this);
    }
}

