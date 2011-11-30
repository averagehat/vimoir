import sys
import os
import cStringIO
import logging
from logging import error, info, debug

import netbeans

def setup_logger(debug):
    """Setup the logger."""
    fmt = logging.Formatter('%(levelname)-7s %(message)s')
    stderr_hdlr = StderrHandler()
    stderr_hdlr.setFormatter(fmt)
    root = logging.getLogger()
    root.addHandler(stderr_hdlr)
    level = logging.ERROR
    if debug:
        level = logging.DEBUG
    root.setLevel(level)

class StderrHandler(logging.StreamHandler):
    """Stderr logging handler."""

    def __init__(self):
        self.strbuf = cStringIO.StringIO()
        self.doflush = True
        logging.StreamHandler.__init__(self, self.strbuf)

    def should_flush(self, doflush):
        """Set flush mode."""
        self.doflush = doflush

    def write(self, string):
        """Write to the StringIO buffer."""
        self.strbuf.write(string)

    def flush(self):
        """Flush to stderr when enabled."""
        if self.doflush:
            value = self.strbuf.getvalue()
            if value:
                print >> sys.stderr, value,
                self.strbuf.truncate(0)

    def close(self):
        """Close the handler."""
        self.flush()
        self.strbuf.close()
        logging.StreamHandler.close(self)

class Phonemic(netbeans.Netbeans):
    def __init__(self, speech, debug):
        netbeans.Netbeans.__init__(self)
        self.speech = speech
        setup_logger(debug)

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

