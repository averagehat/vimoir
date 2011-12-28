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

import java.util.logging.Logger;
import vimoir.netbeans.NetbeansSocket;
import vimoir.netbeans.NetbeansEventHandler;
import vimoir.netbeans.NetbeansBuffer;
import org.sodbeans.phonemic.TextToSpeechFactory;
import org.sodbeans.phonemic.tts.TextToSpeech;

/**
 * A class using <a
 * href="http://sourceforge.net/projects/phonemic/">phonemic</a> to speak
 * audibly Vim buffers content.
 */
public class Phonemic implements NetbeansEventHandler {
    static Logger logger = Logger.getLogger("vimoir.examples");
    /** The type of speech is Object and not TextToSpeech to allow for running
     * without phonemic.jar installed. */
    Object speech;
    NetbeansSocket nbsock;

    /**
     * The constructor.
     *
     * @param nbsock the Netbeans socket
     */
    public Phonemic(NetbeansSocket nbsock) {
        this.nbsock = nbsock;
        this.speech = get_speech();
    }

    /**
     * Speak some text.
     *
     * <p> When phonemic is not available, the messages are printed to stdout
     * instead of being spoken.
     *
     * @param text the text to speak
     */
    void speak(String text) {
        if (this.speech != null)
            ((TextToSpeech) this.speech).speakBlocking(text);
        else
            System.out.println("speak> \"" + text + "\"");
    }

    /**
     * Speak an operational message.
     *
     * <p> This could be done using a second voice obtained from phonemic.
     *
     * @param text the text to speak
     */
    void speak_admin_msg(String text) {
        this.speak(text);
    }

    //-----------------------------------------------------------------------
    //   Events
    //-----------------------------------------------------------------------

    public void event_startupDone() {
        this.speak_admin_msg("Phonemic is connected to Vim");
    }

    public void event_disconnect() {
        this.speak_admin_msg("Phonemic is disconnected from Vim");
    }

    public void event_fileOpened(NetbeansBuffer buf) {
        this.speak_admin_msg("Opening the file " + buf.get_basename());
    }

    public void event_killed(NetbeansBuffer buf) {
        this.speak_admin_msg("Closing the file " + buf.get_basename());
    }

    public void event_error(String msg) {
        this.speak_admin_msg(msg);
    }

    public void event_tick() {}

    //-----------------------------------------------------------------------
    //   Commands
    //-----------------------------------------------------------------------

    /**
     * Handle nbkey commands not matched with a "cmd_keyName" method.
     *
     * <p> Here we just log the event.
     */
    public void default_cmd_processing(String keyName, String args, NetbeansBuffer buf) {
        logger.info("nbkey [" + keyName + ":" + args + ":" + buf.toString() + "]");
    }

    /**
     * This method speaks <i>blahblahblah</i> after a <code>:nbkey speak
     * blahblahblah</code> Vim command.
     *
     * @param args     the remaining string in the <code>:nbkey</code> command
     * @param buf      the buffer instance
     */
    public void cmd_speak(String args, NetbeansBuffer buf) {
        this.speak(args);
    }

    /** Terminate the server. */
    public void cmd_quit(String args, NetbeansBuffer buf) {
        this.nbsock.terminate_server();
    }

    /**
     * Return a phonemic TextToSpeech object.
     *
     * @return <code>null</code> when the phonemic library cannot be found.
     */
    public static Object get_speech() {
        Object speech = null;
        try {
            speech = TextToSpeechFactory.getDefaultTextToSpeech();
        } catch (NoClassDefFoundError ex) {
            logger.severe("cannot find phonemic.jar: " + ex);
        }
        return speech;
    }

    /** Print the command line parameters. */
    public static void main(String[] args) {
        for (int i=0; i < args.length; i++)
            logger.info("parameter " + i + ": " + args[i]);
    }
}

