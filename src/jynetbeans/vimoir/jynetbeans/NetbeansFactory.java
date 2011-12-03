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

/**
 * Object Factory that is used to coerce python module into a Java class
 */
package vimoir.jynetbeans;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyInteger;
import org.python.util.PythonInterpreter;
import vimoir.netbeans.NetbeansType;

class NetbeansFactory {
    private PyObject netbeansClass;

    /**
     * Obtain a reference to the Netbeans class and assign the reference to a
     * Java variable.
     */
    NetbeansFactory() {
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("from netbeans import Netbeans");
        netbeansClass = interpreter.get("Netbeans");
    }

    /**
     * Perform the actual coercion into Java bytecode.
     */
    NetbeansType create(int debug) {
        PyObject netbeansObject = netbeansClass.__call__(new PyInteger(debug));
        return (NetbeansType) netbeansObject.__tojava__(NetbeansType.class);
    }
}
