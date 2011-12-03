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

import java.text.MessageFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class MessageFormatFormatter extends Formatter {

    private static final MessageFormat messageFormat = new MessageFormat("{0}[{1}]: {2}\n");

    public String format(LogRecord record) {
        Object[] arguments = new Object[3];
        arguments[0] = record.getLoggerName();
        arguments[1] = record.getLevel();
        arguments[2] = record.getMessage();
        return messageFormat.format(arguments);
    }

}

