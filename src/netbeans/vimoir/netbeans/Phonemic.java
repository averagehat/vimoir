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

public class Phonemic extends NetbeansClient implements NetbeansClientType {
    static Logger logger;
    public TextToSpeech speech;

    // This constructor is needed because Phonemic in jynetbeans is subclassing
    // this class.
    public Phonemic() {}

    public Phonemic(TextToSpeech speech) {
        // One MUST invoke the superclass constructor.
        super();
        this.speech = speech;
    }

    void speak(String text) {
        // Print on stdout when phonemic is not available.
        if (this.speech != null)
            this.speech.speakBlocking(text);
        else
            System.out.println("speak> \"" + text + "\"");
    }

    void speak_admin_msg(String text) {
        this.speak(text);
    }

    //-----------------------------------------------------------------------
    //   Events
    //-----------------------------------------------------------------------

    public void event_open() {
        this.speak_admin_msg("Phonemic is connected to Vim");
    }

    public void event_close() {
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

    //-----------------------------------------------------------------------
    //   Commands
    //-----------------------------------------------------------------------

    public void default_cmd_processing(String cmd, String args, String pathname, int lnum, int col) {
        // Handle nbkey commands not matched with a "cmd_xxx" method.
        logger.info("nbkey [" + cmd + ":" + args + ":" 
                    + new File(pathname).getName() + ":" + lnum + ":" + col + "]");
    }

    public void cmd_speak(String args, String pathname, int lnum, int col) {
        this.speak(args);
    }

    public static TextToSpeech get_speech() {
        // Setup logging.
        try {
            FileInputStream configFile = new FileInputStream("conf/logging.properties");
            LogManager.getLogManager().readConfiguration(configFile);
        }
        catch(IOException e) {
            Logger.getAnonymousLogger().severe("Could not load logging.properties file");
            Logger.getAnonymousLogger().severe(e.getMessage());
        }
        logger = Logger.getLogger("vimoir.netbeans");

        // Obtain a phonemic TextToSpeech object.
        Object speech = null;
        try {
            speech = TextToSpeechFactory.getDefaultTextToSpeech();
        } catch (NoClassDefFoundError ex) {
            logger.severe("cannot find phonemic.jar: " + ex);
        }
        return (TextToSpeech) speech;
    }

    public static void main(String[] args) {
        // Start netbeans processing.
        Phonemic phonemic = new Phonemic(get_speech());
        phonemic.start();

        // Terminate all Phonemic threads by exiting.
        logger.info("Terminated.");
        System.exit(0);
    }
}

