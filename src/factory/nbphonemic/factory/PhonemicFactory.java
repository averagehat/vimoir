/**
 * Object Factory that is used to coerce python module into a Java class
 */
package nbphonemic.factory;

import org.sodbeans.phonemic.tts.TextToSpeech;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

public class PhonemicFactory {

    private PyObject phonemicClass;

    /**
     * Obtain a reference to the Phonemic class and assign the reference to a
     * Java variable.
     */
    public PhonemicFactory() {
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("from phonemic import Phonemic");
        phonemicClass = interpreter.get("Phonemic");
    }

    /**
     * Perform the actual coercion into Java bytecode.
     */
    public PhonemicType create(TextToSpeech speech) {
        PyObject phonemicObject = phonemicClass.__call__(Py.java2py(speech));
        return (PhonemicType) phonemicObject.__tojava__(PhonemicType.class);
    }

}
