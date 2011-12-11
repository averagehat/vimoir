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

/**
 * The interface methods are call back methods that map to Netbeans events.
 */
public interface NetbeansClientType {

    //-----------------------------------------------------------------------
    //   Events
    //-----------------------------------------------------------------------

    /**
     * Process a <code>startupDone</code> Netbeans event.
     */
    public void event_startupDone();

    /**
     * Process a <code>disconnect</code> Netbeans event.
     */
    public void event_disconnect();

    /**
     * Process a <code>fileOpened</code> Netbeans event.
     *
     * @param pathname full pathname of the file
     */
    public void event_fileOpened(String pathname);

    /**
     * Process a <code>killed</code> Netbeans event.
     *
     * @param pathname full pathname of the file
     */
    public void event_killed(String pathname);

    /**
     * Process a timer event.
     */
    public void event_tick();

    /**
     * Process an error message generated by the Netbeans engine.
     *
     * @param message the error message
     */
    public void event_error(String message);

    /**
     * The default method invoked by the Netbeans engine on a <code>keyAtPos</code>
     * Netbeans event.
     *
     * <p>The Netbeans engine falls back to invoking this method when the
     * NetbeansClientType object does not implement the
     * <code>cmd_keyName</code> method, <code>keyName</code> being the first
     * parameter of the Vim <code>:nbkey</code> command.
     *
     * @param keyName  the first parameter of Vim <code>:nbkey</code> command
     * @param args     the remaining string in the <code>:nbkey</code> command
     * @param pathname the full pathname of the file
     * @param lnum     the cursor line number
     * @param col      the cursor column number
     */
    public void default_cmd_processing(String keyName, String args, String pathname, int lnum, int col);

}
