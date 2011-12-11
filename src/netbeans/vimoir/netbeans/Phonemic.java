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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import org.sodbeans.phonemic.TextToSpeechFactory;
import org.sodbeans.phonemic.tts.TextToSpeech;

/**
 * A class using <a
 * href="http://sourceforge.net/projects/phonemic/">phonemic</a> to speak
 * audibly Vim buffers content.
 */
public class Phonemic extends NetbeansClient implements NetbeansClientType {
    static Logger logger = Logger.getLogger("vimoir.netbeans");
    /** The type of speech is Object and not TextToSpeech to allow for running
     * without phonemic.jar installed. */
    public Object speech;

    public Phonemic() throws IOException {}

    /**
     * The constructor <b>MUST</b> invoke the superclass constructor.
     *
     * @param speech the phonemic TextToSpeech instance or <code>null</code>
     */
    public Phonemic(Object speech) throws IOException {
        super();
        this.speech = speech;
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

    public void event_fileOpened(String pathname) {
        this.speak_admin_msg("Opening the file " + new File(pathname).getName());
    }

    public void event_killed(String pathname) {
        this.speak_admin_msg("Closing the file " + new File(pathname).getName());
    }

    public void event_error(String msg) {
        this.speak_admin_msg(msg);
    }

    public void event_tick() {}

    /**
     * Handle nbkey commands not matched with a "cmd_keyName" method.
     *
     * <p> Here we just log the event.
     */
    public void default_cmd_processing(String keyName, String args, String pathname, int lnum, int col) {
        logger.info("nbkey [" + keyName + ":" + args + ":" 
                    + new File(pathname).getName() + ":" + lnum + ":" + col + "]");
    }

    /**
     * This method speaks <i>blahblahblah</i> after a <code>:nbkey speak
     * blahblahblah</code> Vim command.
     *
     * @param args     the remaining string in the <code>:nbkey</code> command
     * @param pathname the full pathname of the file
     * @param lnum     the cursor line number
     * @param col      the cursor column number
     */
    public void cmd_speak(String args, String pathname, int lnum, int col) {
        this.speak(args);
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

    /**
     * Run a Phonemic instance.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // Start netbeans processing.
        Phonemic phonemic = new Phonemic(get_speech());
        phonemic.start();

        // Terminate all Phonemic threads by exiting.
        logger.info("Terminated.");
        System.exit(0);
    }
}

