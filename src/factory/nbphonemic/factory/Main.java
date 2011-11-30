package nbphonemic.factory;

import org.sodbeans.phonemic.TextToSpeechFactory;
import org.sodbeans.phonemic.tts.TextToSpeech;
import org.python.util.PythonInterpreter;

public class Main {

    public static void main(String[] args) {
        // Add the current directory and jython.jar directory to jython path.
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("import sys");
        interpreter.exec("sys.path[0:0] = ['']");
        interpreter.exec("sys.path[0:0] = ['" + args[0] + "']");

        PhonemicFactory phonemicFactory = new PhonemicFactory();
        TextToSpeech speech = TextToSpeechFactory.getDefaultTextToSpeech();
        PhonemicType nbsock = phonemicFactory.create(speech);
        ServerFactory serverFactory = new ServerFactory();
        ServerType nbserver = serverFactory.create(nbsock, 1);
        nbserver.bind_listen();
        nbserver.loop();

        // Terminate all Phonemic threads by exiting.
        System.err.println("Terminated");
        System.exit(0);
    }

}

