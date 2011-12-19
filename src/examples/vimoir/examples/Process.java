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

package vimoir.examples;

import java.util.Arrays;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import vimoir.netbeans.NetbeansClient;
import vimoir.netbeans.NetbeansEventHandler;
import vimoir.netbeans.NetbeansEngine;
import vimoir.netbeans.NetbeansBuffer;

/**
 * Run a process from a Vim <code>:nbkey run</code> command in a worker thread.
 *
 * <p>This class extends {@link vimoir.netbeans.NetbeansClient} so as to
 * implement only the methods it is interested in. The process is run in a
 * thread so as not to block I/O on the Netbeans socket. On the process
 * termination, the process output from stdout and stderr is displayed in a Vim
 * balloon.
 * <p>For example to run a python command that sleeps 2 seconds and prints
 * 'Ok', run in Vim:
 *
 * <p><code>:nbkey run python -c "import time; time.sleep(2); print \"Ok\""</code>
 */
public class Process extends NetbeansClient implements NetbeansEventHandler {

    /**
     * Constructor.
     *
     * @param nbsock the Netbeans engine
     */
    public Process(NetbeansEngine nbsock) {
        super(nbsock);
    }

    /**
     * Run a process in a worker thread.
     *
     * @param args  the remaining string of the <code>:nbkey run</code> command
     * @param buf   the buffer instance (not used here)
     */
    public void cmd_run(final String args, NetbeansBuffer buf) {
        final String[] command = this.nbsock.split_quoted_string(args);
        Thread thread = new Thread(new Runnable() {
                public void run() {
                    String output = "";
                    try {
                        java.lang.Process p = new ProcessBuilder(Arrays.asList(command))
                                            .redirectErrorStream(true).start();
                        p.waitFor();
                        BufferedReader br = new BufferedReader(
                                            new InputStreamReader(p.getInputStream()));
                        String line = null;
                        while ((line = br.readLine()) != null)
                            output += line + "\n";
                    } catch (IOException e) {
                        nbsock.send_cmd(null, "showBalloon", nbsock.quote(e.toString()));
                        return;
                    } catch (InterruptedException e) {
                        nbsock.send_cmd(null, "showBalloon", nbsock.quote(e.toString()));
                        return;
                    }
                    String msg = "Result of process '" + args + "':\n" + output;
                    nbsock.send_cmd(null, "showBalloon", nbsock.quote(msg));
                }
        });
        thread.start();
    }
}

