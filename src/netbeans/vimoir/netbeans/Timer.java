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

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * A Timer registered with a Selector.
 *
 * This is an abstract class. You must derive from this class, and implement
 * the handle_tick method.
 */
abstract class Timer extends Dispatcher {

    Timer() throws java.io.IOException {
        super();
        this.initTimer();
    }

    Timer(Selector selector) throws java.io.IOException {
        super(selector);
        this.initTimer();
    }

    void initTimer() throws java.io.IOException {
        // instantiate a fake SocketChannel to register the timer
        this.createSocket(false);
    }

    void handle_read() {}
    void handle_write() {}
    void handle_accept(SocketChannel channel) {}
    void handle_connect() {}

}
