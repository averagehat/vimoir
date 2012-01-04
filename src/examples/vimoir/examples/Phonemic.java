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
        // Set this buffer as "owned" by Netbeans so as to get buttonRelease events.
        this.nbsock.send_cmd(buf, "netbeansBuffer", "T");
        this.speak_admin_msg("Opening the file " + buf.get_basename());
    }

    public void event_killed(NetbeansBuffer buf) {
        this.speak_admin_msg("Closing the file " + buf.get_basename());
    }

    public void event_version(String version) {
        this.speak_admin_msg("Vim netbeans version is " + version);
    }

    public void event_balloonText(String text) {
        this.speak_admin_msg(text);
    }

    public void event_buttonRelease(NetbeansBuffer buf, int button) {
        this.speak_admin_msg("Button " + button +
                " at line " + buf.lnum + " and column " + buf.col);
    }

    public void event_keyCommand(NetbeansBuffer buf, String keyName) {
        this.speak_admin_msg(keyName);
    }

    public void event_newDotAndMark(NetbeansBuffer buf) {
        this.speak_admin_msg("cursor offset at " + buf.offset);
    }

    public void event_insert(NetbeansBuffer buf, String text) {
        this.speak_admin_msg("The following text was inserted"
                        + " at byte offset " + buf.offset + ": " + text);
    }

    public void event_remove(NetbeansBuffer buf, int length) {
        this.speak_admin_msg(length
            + " bytes of text were removed at byte offset " + buf.offset);
    }

    public void event_save(NetbeansBuffer buf) {
        this.speak_admin_msg("Buffer " + buf.get_basename() + " has been saved.");
    }

    public void event_error(String msg) {
        this.speak_admin_msg(msg);
    }

    public void event_tick() {}

    //-----------------------------------------------------------------------
    //   Commands
    //-----------------------------------------------------------------------

    /**
     * Handle nbkey commands not matched with a "cmd_&lt;keyName&gt;()" method.
     *
     * <p> Here we just log the event.
     */
    public void default_cmd_processing(NetbeansBuffer buf, String keyName, String args) {
        logger.info("nbkey [" + keyName + ":" + args + ":" + buf.toString() + "]");
    }

    /**
     * This method speaks <i>blahblahblah</i> after a <code>:nbkey speak
     * blahblahblah</code> Vim command.
     *
     * @param buf      the buffer instance
     * @param args     the remaining string in the <code>:nbkey</code> command
     */
    public void cmd_speak(NetbeansBuffer buf, String args) {
        this.speak(args);
    }

    /** Terminate the server. */
    public void cmd_quit(NetbeansBuffer buf, String args) {
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

