/**
 * Object Factory that is used to coerce python module into a Java class
 */
package vimoir.jynetbeans;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyInteger;
import org.python.util.PythonInterpreter;

public class ServerFactory {

    private PyObject serverClass;

    /**
     * Obtain a reference to the Server class and assign the reference to a
     * Java variable.
     */
    public ServerFactory() {
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("from netbeans import Server");
        serverClass = interpreter.get("Server");
    }

    /**
     * Perform the actual coercion into Java bytecode.
     */
    public ServerType create(PhonemicType nbsock, int debug) {
        PyObject serverObject = serverClass.__call__(Py.java2py(nbsock),
                                                         new PyInteger(debug));
        return (ServerType) serverObject.__tojava__(ServerType.class);
    }

}
