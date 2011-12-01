import os
from logging import error, info, debug

try:
    from vimoir.phonemic import PhonemicType
except ImportError:
    PhonemicType = object
import netbeans

class Phonemic(netbeans.Netbeans, PhonemicType):
    def __init__(self, speech):
        netbeans.Netbeans.__init__(self)
        self.speech = speech

    def second_voice(self, text):
        self.speech.speakBlocking(text)

    #-----------------------------------------------------------------------
    #   Events
    #-----------------------------------------------------------------------

    def event_open(self):
        self.second_voice('Phonemic is connected to Vim')

    def event_close(self):
        self.second_voice('Phonemic is disconnected from Vim')

    def event_fileOpened(self, pathname):
        self.second_voice('Opening the file %s' % os.path.basename(pathname))

    def event_killed(self, pathname):
        self.second_voice('Closing the file %s' % os.path.basename(pathname))

    #-----------------------------------------------------------------------
    #   Commands
    #-----------------------------------------------------------------------

    def show_balloon(self, text):
        """Override show_balloon."""
        self.second_voice(text)

    def default_cmd_processing(self, cmd, args, buf, lnum, col):
        """Handle nbkey commands not matched with a 'cmd_xxx' method."""
        info('nbkey: %s', (cmd, args, buf.name, lnum, col))

    def cmd_speak(self, args, buf, lnum, col):
        self.speech.speakBlocking(args)

