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

import netbeans

def usage():
    print >> sys.stderr, ('usage: %s /path/to/phonemic.jar'
                            % os.path.basename(sys.argv[0]))
    sys.exit(1)

def get_speech():
    """Return None when run by python or phonemic.jar cannot be found."""
    try:
        import java
    except ImportError:
        return None

    try:
        sys.path.append(sys.argv[1])
        import org.sodbeans.phonemic.TextToSpeechFactory as TextToSpeechFactory
    except IndexError:
        usage()
    except java.lang.UnsatisfiedLinkError:
        pass
    except ImportError, err:
        print >> sys.stderr, 'cannot find phonemic.jar: %s' % str(err)
        return None
    else:
        speech = TextToSpeechFactory.getDefaultTextToSpeech()
        if speech.canSetSpeed():
            print >> sys.stderr, 'speech speed: %f' % speech.getSpeed()
            pass
        return speech
    sys.exit(1)

class NetbeansClient(object):
    def __init__(self):
        self.nbsock = netbeans.Netbeans(debug=1)

    def start(self):
        self.nbsock.start(self)

class Phonemic(NetbeansClient):
    def __init__(self, speech):
        NetbeansClient.__init__(self)
        self.speech = speech

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

    def event_open(self):
        self.speak_admin_msg('Phonemic is connected to Vim')

    def event_close(self):
        self.speak_admin_msg('Phonemic is disconnected from Vim')

    def event_fileOpened(self, pathname):
        self.speak_admin_msg('Opening the file %s' % os.path.basename(pathname))

    def event_killed(self, pathname):
        self.speak_admin_msg('Closing the file %s' % os.path.basename(pathname))

    def event_error(self, msg):
        self.speak_admin_msg(msg)

    #-----------------------------------------------------------------------
    #   Commands
    #-----------------------------------------------------------------------

    def default_cmd_processing(self, cmd, args, bufname, lnum, col):
        """Handle nbkey commands not matched with a 'cmd_xxx' method."""
        info('nbkey: %s', (cmd, args, bufname, lnum, col))

    def cmd_speak(self, args, buf, lnum, col):
        self.speak(args)

def main():
    phonemic = Phonemic(get_speech())
    phonemic.start()
    # Terminate all Phonemic threads by exiting.
    print >> sys.stderr, 'Terminated.'
    sys.exit(0)

if __name__ == "__main__":
    main()

