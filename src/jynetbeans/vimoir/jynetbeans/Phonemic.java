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

package vimoir.jynetbeans;

import java.util.logging.Logger;
import java.util.logging.Level;
import org.python.util.PythonInterpreter;
import vimoir.netbeans.NetbeansClientType;
import vimoir.netbeans.NetbeansType;

/**
 * A class that speaks audibly Vim buffers content.
 */
public class Phonemic extends vimoir.netbeans.Phonemic implements NetbeansClientType {
    static Logger logger;

    public Phonemic(Object speech) {
        logger = Logger.getLogger("vimoir.jynetbeans");
        Level level = logger.getLevel();
        int debug = 0;
        if (level == Level.ALL)
            debug = 1;

        // Instantiate Netbeans
        NetbeansFactory netbeansFactory = new NetbeansFactory();
        NetbeansType nbsock = netbeansFactory.create(debug);
        this.setNbsock(nbsock);

        this.speech = speech;
    }

    public static void main(String[] args) {
        // Add the current directory and jython.jar directory to jython path.
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("import sys");
        interpreter.exec("sys.path[0:0] = ['']");
        interpreter.exec("sys.path[0:0] = ['" + args[0] + "']");

        // Start netbeans processing.
        Phonemic phonemic = new Phonemic(vimoir.netbeans.Phonemic.get_speech());
        phonemic.start();

        // Terminate all Phonemic threads by exiting.
        logger.info("Terminated.");
        System.exit(0);
    }
}

