# Copyright 2011 Xavier de Gaye
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys
import os
from logging import error, info, debug

def get_speech():
    """Return None when run by python or phonemic.jar cannot be found."""
    try:
        import java
    except ImportError:
        return None

    try:
        sys.path.extend(sys.argv[-1:])
        import org.sodbeans.phonemic.TextToSpeechFactory as TextToSpeechFactory
    except java.lang.UnsatisfiedLinkError:
        pass
    except ImportError, err:
        print >> sys.stderr, 'cannot find phonemic.jar: %s' % str(err)
        return None
    else:
        speech = TextToSpeechFactory.getDefaultTextToSpeech()
        return speech
    sys.exit(1)

class Phonemic(object):
    def __init__(self, nbsock):
        self.nbsock = nbsock
        self.speech = get_speech()

    def speak(self, text):
        if self.speech:
            self.speech.speakBlocking(text)
        else:
            # Print on stdout when phonemic is not available.
            sys.stdout.write('speak> "' + text + '"' + os.linesep)

    def speak_admin_msg(self, text):
        self.speak(text)

    #-----------------------------------------------------------------------
    #   Events
    #-----------------------------------------------------------------------

    def event_startupDone(self):
        self.speak_admin_msg('Phonemic is connected to Vim')

    def event_disconnect(self):
        self.speak_admin_msg('Phonemic is disconnected from Vim')

    def event_fileOpened(self, buf):
        self.speak_admin_msg('Opening the file %s' % buf.get_basename())

    def event_killed(self, buf):
        self.speak_admin_msg('Closing the file %s' % buf.get_basename())

    def event_tick(self):
        pass

    def event_error(self, msg):
        self.speak_admin_msg(msg)

    #-----------------------------------------------------------------------
    #   Commands
    #-----------------------------------------------------------------------

    def default_cmd_processing(self, cmd, args, buf):
        """Handle nbkey commands not matched with a 'cmd_xxx' method."""
        info('nbkey: %s', (cmd, args, buf))

    def cmd_speak(self, args, buf):
        self.speak(args)

    def cmd_quit(self, args, buf):
        """Terminate the server."""
        self.nbsock.terminate_server()
