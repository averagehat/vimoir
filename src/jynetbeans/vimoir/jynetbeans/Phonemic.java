package vimoir.jynetbeans;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.Level;
import org.sodbeans.phonemic.TextToSpeechFactory;
import org.sodbeans.phonemic.tts.TextToSpeech;
import org.python.util.PythonInterpreter;

public class Phonemic {

    public static void main(String[] args) {
        try {
            FileInputStream configFile = new FileInputStream("conf/logging.properties");
            LogManager.getLogManager().readConfiguration(configFile);
        }
        catch(IOException e) {
            Logger.getAnonymousLogger().severe("Could not load logging.properties file");
            Logger.getAnonymousLogger().severe(e.getMessage());
        }
        Logger logger = Logger.getLogger("vimoir.jynetbeans");
        Level level = logger.getLevel();
        int debug = 0;
        if (level == Level.ALL)
            debug = 1;

        // Add the current directory and jython.jar directory to jython path.
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("import sys");
        interpreter.exec("sys.path[0:0] = ['']");
        interpreter.exec("sys.path[0:0] = ['" + args[0] + "']");

        PhonemicFactory phonemicFactory = new PhonemicFactory();
        Object speech = null;
        try {
            speech = TextToSpeechFactory.getDefaultTextToSpeech();
        } catch (NoClassDefFoundError ex) {
            logger.severe("cannot find phonemic.jar: " + ex);
        }
        PhonemicType nbsock = phonemicFactory.create((TextToSpeech) speech);
        ServerFactory serverFactory = new ServerFactory();
        ServerType nbserver = serverFactory.create(nbsock, debug);
        nbserver.bind_listen();
        nbserver.loop();

        // Terminate all Phonemic threads by exiting.
        logger.info("Terminated");
        System.exit(0);
    }

}

