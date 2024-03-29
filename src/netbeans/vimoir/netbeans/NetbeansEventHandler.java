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
 *
 * <p> Note that among all the methods that take a NetbeansBuffer object as
 * argument, only <code>event_fileOpened</code> may get a <code>null</code>
 * NetbeansBuffer.
 */
public interface NetbeansEventHandler {

    //-----------------------------------------------------------------------
    //   Events
    //-----------------------------------------------------------------------

    /**
     * Process a <code>startupDone</code> Netbeans event.
     *
     * <p> Vim has finished its startup work and is ready for editing files.
     */
    public void event_startupDone();

    /**
     * Process a <code>disconnect</code> Netbeans event.
     *
     * <p> Vim is exiting, the client must not write more commands.
     */
    public void event_disconnect();

    /**
     * Process a <code>fileOpened</code> Netbeans event.
     *
     * <p> A file was opened by the Vim user.
     *
     * @param buf the buffer which may be <code>null</code> when the current
     *            buffer in Vim is the <code>[No Name]</code> buffer.
     */
    public void event_fileOpened(NetbeansBuffer buf);

    /**
     * Process a <code>killed</code> Netbeans event.
     *
     * <p> A file was deleted or wiped out by the Vim user and the buffer
     * annotations have been removed.  The buffer is not registered anymore.
     *
     * @param buf the buffer
     */
    public void event_killed(NetbeansBuffer buf);

    /**
     * Process a <code>version</code> Netbeans event.
     *
     * <p> Report the version of the interface implementation.
     *
     * @param version of the Vim Netbeans protocol
     */
    public void event_version(String version);

    /**
     * Process a <code>balloonText</code> Netbeans event.
     *
     * <p> Report the text under the mouse pointer. The text may be a single
     * word or a part of the buffer selected in visual-mode.
     *
     * @param text under the mouse pointer
     */
    public void event_balloonText(String text);

    /**
     * Process a <code>buttonRelease</code> Netbeans event.
     *
     * <p> Report which button was pressed and the mouse pointer location at
     * the time of the release as the buffer <code>offset</code>.
     *
     * @param buf the buffer
     * @param button button number
     */
    public void event_buttonRelease(NetbeansBuffer buf, int button);

    /**
     * Process a <code>keyCommand</code> Netbeans event.
     *
     * <p> Report a special key being pressed with name 'keyName'.
     *
     * @param buf the buffer
     * @param keyName the <code>nbkey</code> parameters
     */
    public void event_keyCommand(NetbeansBuffer buf, String keyName);

    /**
     * Process a <code>newDotAndMark</code> Netbeans event.
     *
     * <p> Report the cursor position as the buffer <code>offset</code>.
     *
     * @param buf the buffer
     */
    public void event_newDotAndMark(NetbeansBuffer buf);

    /**
     * Process an <code>insert</code> Netbeans event.
     *
     * <p> Text <code>text</code> has been inserted in Vim at byte position
     * <code>offset</code>.
     *
     * @param buf the buffer
     * @param text that has been inserted
     */
    public void event_insert(NetbeansBuffer buf, String text);

    /**
     * Process a <code>remove</code> Netbeans event.
     *
     * <p> <code>length</code> bytes of text were deleted in Vim at position
     * <code>offset</code>.
     *
     * @param buf the buffer
     * @param length the number of bytes that have been deleted
     */
    public void event_remove(NetbeansBuffer buf, int length);

    /**
     * Process a <code>save</code> Netbeans event.
     *
     * <p> The buffer has been saved and is now unmodified.
     *
     * @param buf the buffer
     */
    public void event_save(NetbeansBuffer buf);

    /**
     * Process a timer event.
     *
     * <p> The periodicity of the invocation of this method is defined by the
     * <code>vimoir.netbeans.user_interval</code> property.  See the
     * <code>vimoir.properties</code> file.
     */
    public void event_tick();

    //-----------------------------------------------------------------------
    //   Commands
    //-----------------------------------------------------------------------

    /**
     * The default method invoked by the Netbeans socket on a <code>keyAtPos</code>
     * Netbeans event.
     *
     * <p>The Netbeans socket falls back to invoking this method when the
     * NetbeansEventHandler object does not implement the
     * <code>cmd_&lt;keyName&gt;</code> method, <code>keyName</code> being the
     * first token of the Vim <code>:nbkey</code> command.
     *
     * @param buf      the buffer
     * @param keyName  the first parameter of Vim <code>:nbkey</code> command
     * @param args     the remaining string in the <code>:nbkey</code> command
     */
    public void default_cmd_processing(NetbeansBuffer buf, String keyName, String args);
}
