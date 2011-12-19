#! /usr/bin/env python
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
import time
import re
import optparse
import asyncore
import asynchat
import socket
import cStringIO
import logging
import ConfigParser
from logging import error, info, debug

VERSION=0.1 # no spaces
SECTION_NAME = 'vimoir properties'
DEFAULTS = {
    'vimoir.netbeans.python.client': 'src.examples.phonemic.Phonemic',
    'vimoir.netbeans.host': '',
    'vimoir.netbeans.port': '3219',
    'vimoir.netbeans.password': 'changeme',
    'vimoir.netbeans.encoding': 'UTF-8',
    'vimoir.netbeans.timeout': '20',
    'vimoir.netbeans.user_interval': '200',
}
RE_ESCAPE = ur'["\n\t\r\\]'                                             \
            ur'# RE: escaped characters in a string'
RE_UNESCAPE = ur'\\["ntr\\]'                                            \
              ur'# RE: escaped characters in a quoted string'
RE_AUTH = ur'^\s*AUTH\s*(?P<passwd>\S+)\s*$'                            \
          ur'# RE: password authentication'
RE_RESPONSE = ur'^\s*(?P<seqno>\d+)\s*(?P<args>.*)\s*$'                 \
              ur'# RE: a netbeans response'
RE_EVENT = ur'^\s*(?P<buf_id>\d+):(?P<event>\S+)=(?P<seqno>\d+)'        \
           ur'\s*(?P<args>.*)\s*$'                                      \
           ur'# RE: a netbeans event message'
RE_LNUMCOL = ur'^(?P<lnum>\d+)/(?P<col>\d+)'                            \
             ur'# RE: lnum/col'
RE_TOKEN_SPLIT = r'\s*"((?:\\"|[^"])+)"\s*|\s*([^ "]+)\s*'              \
                 r'# RE: split a string in tokens, handling quotes'

# compile regexps
re_escape = re.compile(RE_ESCAPE, re.VERBOSE)
re_unescape = re.compile(RE_UNESCAPE, re.VERBOSE)
re_auth = re.compile(RE_AUTH, re.VERBOSE)
re_response = re.compile(RE_RESPONSE, re.VERBOSE)
re_event = re.compile(RE_EVENT, re.VERBOSE)
re_lnumcol = re.compile(RE_LNUMCOL, re.VERBOSE)
re_token_split = re.compile(RE_TOKEN_SPLIT, re.VERBOSE)

def escape_char(matchobj):
    """Escape special characters in string."""
    if matchobj.group(0) == u'"': return ur'\"'
    if matchobj.group(0) == u'\n': return ur'\n'
    if matchobj.group(0) == u'\t': return ur'\t'
    if matchobj.group(0) == u'\r': return ur'\r'
    if matchobj.group(0) == u'\\': return ur'\\'
    assert False

def unescape_char(matchobj):
    """Remove escape on special characters in quoted string."""
    if matchobj.group(0) == ur'\"': return u'"'
    if matchobj.group(0) == ur'\n': return u'\n'
    if matchobj.group(0) == ur'\t': return u'\t'
    if matchobj.group(0) == ur'\r': return u'\r'
    if matchobj.group(0) == ur'\\': return u'\\'
    assert False

def unquote(msg):
    """Remove escapes from escaped characters in a quoted string."""
    return u'%s' % re_unescape.sub(unescape_char, msg)

def evt_ignore(buf_id, msg, arg_list):
    """Ignore not implemented received events."""
    pass

def parse_msg(msg):
    """Parse a received netbeans message.

    Return the (None,) tuple or the tuple:
        is_event: boolean
            True: an event - False: a reply
        buf_id: int
            netbeans buffer number
        event: str
            event name
        seqno: int
            netbeans sequence number
        nbstring: str
            the netbeans string
        arg_list: list
            list of remaining args after the netbeans string

    """
    matchobj = re_event.match(msg)
    if matchobj:
        # an event
        bufid_name = matchobj.group(u'buf_id')
        event = matchobj.group(u'event')
    else:
        # a reply
        bufid_name = u'0'
        event = u''
        matchobj = re_response.match(msg)
    if not matchobj:
        error(u'discarding invalid netbeans message: "%s"', msg)
        return (None,)

    seqno = matchobj.group(u'seqno')
    args = matchobj.group(u'args').strip()
    try:
        buf_id = int(bufid_name)
        seqno = int(seqno)
    except ValueError:
        assert False, 'error in regexp'

    # a netbeans string
    nbstring = u''
    if args and args[0] == u"\"":
        end = args.rfind(u"\"")
        if end != -1 and end != 0:
            nbstring = args[1:end]
            # Do not unquote nbkey parameter twice since vim already parses
            # function parameters as strings (see :help expr-quote).
            if event != u'keyAtPos':
                nbstring = unquote(nbstring)
        else:
            end = -1
    else:
        end = -1
    arg_list = args[end+1:].split()

    return (matchobj.re is re_event), buf_id, event, seqno, nbstring, arg_list

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

def load_properties(filename):
    """Load configuration properties."""
    opts = RawConfigParser(DEFAULTS)
    try:
        f = FileLike(filename)
        opts.readfp(f)
        f.close()
        info('loading properties from %s', filename)
    except IOError, e:
        error('cannot load properties, using defaults: %s', e)
        opts.add_section(SECTION_NAME)
    except ConfigParser.ParsingError, e:
        error(e)
    return opts

class RawConfigParser(ConfigParser.RawConfigParser):
    """A RawConfigParser subclass with a getter and no section parameter."""
    def get(self, option):
        return ConfigParser.RawConfigParser.get(self, SECTION_NAME, option)

class FileLike(object):
    """File like object to read a java properties file with RawConfigParser."""
    def __init__(self, fname):
        self.name = fname
        self.f = None

    def readline(self):
        if not self.f:
            self.f = open(self.name)
            return '[%s]\n' % SECTION_NAME
        return self.f.readline()

    def close(self):
        if self.f:
            self.f.close()

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

class Error(Exception):
    """Base class for exceptions."""

class NetbeansClient(object):
    """This class implements all the NetbeansEventHandler methods.

    This class exists so as to be subclassed by implementations that want to
    ignore most events and to implement only the ones they are interested in.
    """
    def __init__(self, nbsock):
        self.nbsock = nbsock

    #-----------------------------------------------------------------------
    #   Events
    #-----------------------------------------------------------------------

    def event_startupDone(self):
        """Terminate the server on startup to allow only one Netbeans session."""
        self.nbsock.terminate_server()

    def event_disconnect(self):
        pass

    def event_fileOpened(self, buf):
        pass

    def event_killed(self, buf):
        pass

    def event_tick(self):
        pass

    def event_error(self, msg):
        pass

    #-----------------------------------------------------------------------
    #   Commands
    #-----------------------------------------------------------------------

    def default_cmd_processing(self, cmd, args, buf):
        pass

class Reply(object):
    """Abstract class. A Reply instance is a callable used to process
    the result of a  function call in the reply received from netbeans.

    Instance attributes:
        buf: NetbeansBuffer
            the buffer in use when the function is invoked
        seqno: int
            netbeans sequence number
        nbsock: netbeans.Netbeans
            the netbeans asynchat socket
    """

    def __init__(self, buf, seqno, nbsock):
        self.buf = buf
        self.seqno = seqno
        self.nbsock = nbsock

    def __call__(self, seqno, nbstring, arg_list):
        """Process the netbeans reply."""
        pass

class Server(asyncore.dispatcher):
    def __init__(self, host, port, props_file):
        asyncore.dispatcher.__init__(self)
        self.props_file = props_file
        self.nbsock = None
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
        self.bind_listen(host, port)

    def bind_listen(self, host, port):
        self.set_reuse_addr()
        self.bind((host, int(port)))
        self.listen(1)
        info('listening on: %s', (host, port))

    def handle_accept(self):
        """Accept the connection from Vim."""
        conn, addr = self.socket.accept()
        if self.nbsock and self.nbsock.connected:
            conn.close()
            info('rejecting connection from %s: netbeans already connected',
                                                                    addr)
            return

        # get the class to instantiate
        opts = load_properties(self.props_file)
        name = opts.get('vimoir.netbeans.python.client')
        idx = name.rfind('.')
        if idx == -1:
            stmt = "import %s as clazz" % name
        else:
            stmt = "from %s import %s as clazz" % (name[:idx], name[idx+1:])
        try:
            exec stmt
        except ImportError, e:
            error(e)
            sys.exit(1)

        self.nbsock = Netbeans(self, opts)
        client = clazz(self.nbsock)
        self.nbsock.set_client(client)
        conn.setblocking(0)
        self.nbsock.set_socket(conn)
        self.nbsock.connected = True
        info('%s connected to %s', clazz.__name__, addr)

    def handle_tick(self):
        pass

class Netbeans(asynchat.async_chat):
    def __init__(self, server, opts):
        asynchat.async_chat.__init__(self)
        self.server = server
        self.opts = opts
        self.client = None
        self.set_terminator(u'\n')
        self.ibuff = []
        self.ready = False
        self.reply_fifo = asynchat.fifo()
        self._bset = BufferSet(self)
        self.seqno = 0
        self.last_seqno = 0
        self.encoding = opts.get('vimoir.netbeans.encoding')

    def __getattr__(self, attr):
        """Override buggy __getattr__ asyncore method."""
        try:
            retattr = getattr(self.socket, attr)
        except AttributeError:
            msg = ("%s instance has no attribute '%s'"
                                 %(self.__class__.__name__, attr))
            if not attr.startswith('evt_') and not attr.startswith('cmd_'):
                error(msg)
            raise AttributeError(msg)
        return retattr

    def set_client(self, client):
        self.client = client

    def terminate_server(self):
        """Terminate the server."""
        info("terminating the server")
        self.server.close()

    def close(self):
        if not self.connected:
            return
        asynchat.async_chat.close(self)
        self.connected = False

    def collect_incoming_data(self, data):
        self.ibuff.append(unicode(data, self.encoding))

    def found_terminator(self):
        """Process new line terminated netbeans message."""
        msg = u''.join(self.ibuff)
        self.ibuff = []
        debug(msg)

        if not self.ready:
            self.open_session(msg)
            return

        # Handle variable number of elements in returned tuple.
        is_event, buf_id, event, seqno, nbstring, arg_list = (
                (lambda a, b=None, c=None, d=None, e=None, f=None:
                            (a, b, c, d, e, f))(*parse_msg(msg)))

        if is_event is None:
            # Ignore invalid message.
            pass
        elif is_event:
            evt_handler = getattr(self, "evt_%s" % event, evt_ignore)
            evt_handler(buf_id, nbstring, arg_list)

        # A function reply: process the reply.
        else:
            # Vim may send multiple replies for one function request.
            if seqno == self.last_seqno:
                return

            if self.reply_fifo.is_empty():
                raise Error(
                        'got a reply with no matching function request')
            n, reply = self.reply_fifo.pop()
            reply(seqno, nbstring, arg_list)
            self.last_seqno = seqno

    def open_session(self, msg):
        """Process initial netbeans messages."""
        # 'AUTH changeme'
        matchobj = re_auth.match(msg)
        if matchobj:
            password = matchobj.group(u'passwd')
            if (password == self.opts.get('vimoir.netbeans.password')):
                return
            else:
                raise Error('invalid password: "%s"' % password)
        # '0:version=0 "2.3"'
        # '0:startupDone=0'
        else:
            # Handle variable number of elements in returned tuple.
            is_event, buf_id, event, seqno, nbstring, arg_list = (
                    (lambda a, b=None, c=None, d=None, e=None, f=None:
                                (a, b, c, d, e, f))(*parse_msg(msg)))

            if is_event:
                if event == u"version":
                    return
                elif event == u"startupDone":
                    self.ready = True
                    self.client.event_startupDone()
                    return
        raise Error('received unexpected message: "%s"' % msg)

    def get_buffer(pathname):
        return self._bset[pathname]

    #-----------------------------------------------------------------------
    #   Events
    #-----------------------------------------------------------------------
    def evt_disconnect(self, buf_id, nbstring, arg_list):
        """Process a disconnect netbeans event."""
        self.client.event_disconnect()
        self.close()

    def evt_fileOpened(self, buf_id, pathname, arg_list):
        """A file was opened by the user."""
        if pathname:
            if os.path.isabs(pathname):
                buf = self._bset[pathname]
                if buf.buf_id != buf_id:
                    if buf_id == 0:
                        buf.register(editFile=False)
                    else:
                        error('got fileOpened with wrong bufId')
                        return
                self.client.event_fileOpened(buf)
            else:
                error('absolute pathname required')
        else:
            self.client.event_error(
                u'You cannot use netbeans on a "[No Name]" file.\n'
                u'Please, edit a file.'
                )

    def evt_keyAtPos(self, buf_id, nbstring, arg_list):
        """Process a keyAtPos netbeans event."""
        buf = self._bset.getbuf_at(buf_id)
        if buf is None:
            error('invalid bufId: "%d" in keyAtPos', buf_id)
        elif not nbstring:
            debug('empty string in keyAtPos')
        elif len(arg_list) != 2:
            error('invalid arg in keyAtPos')
        else:
            matchobj = re_lnumcol.match(arg_list[1])
            if not matchobj:
                error('invalid lnum/col: %s', arg_list[1])
            else:
                buf.lnum = int(matchobj.group(u'lnum'))
                buf.col = int(matchobj.group(u'col'))
                cmd, args = (lambda a=u'', b=u'':
                                    (a, b))(*nbstring.split(None, 1))
                try:
                    method = getattr(self.client, 'cmd_%s' % cmd)
                except AttributeError:
                    self.client.default_cmd_processing(cmd, args, buf)
                else:
                    method(args, buf)

    def evt_killed(self, buf_id, nbstring, arg_list):
        """A file was deleted or wiped out by the user."""
        buf = self._bset.getbuf_at(buf_id)
        if buf is None:
            error('invalid bufId: "%s" in killed', buf_id)
        else:
            buf.registered = False
            self.client.event_killed(buf)

    def handle_tick(self):
        self.client.event_tick()

    #-----------------------------------------------------------------------
    #   Commands - Functions
    #-----------------------------------------------------------------------

    def send_cmd(self, buf, cmd, args=u''):
        """Send a command to Vim."""
        self.send_request(u'%d:%s!%d%s%s\n', buf, cmd, args)

    def send_function(self, buf, function, args=u''):
        """Send a function call to Vim."""
        try:
            clss = eval('%sReply' % function)
        except NameError:
            assert False, 'internal error, no reply class for %s' % function
        assert issubclass(clss, Reply)
        reply = clss(buf, self.seqno + 1, self)
        self.reply_fifo.push(reply)
        self.send_request(u'%d:%s/%d%s%s\n', buf, function, args)

    def send_request(self, fmt, buf, request, args):
        """Send a netbeans function or command."""
        self.seqno += 1
        buf_id = 0
        space = u' '
        if isinstance(buf, NetbeansBuffer):
            buf_id = buf.buf_id
        if not args:
            space = u''
        msg = fmt % (buf_id, request, self.seqno, space, args)
        if self.connected:
            self.push(msg.encode(self.encoding))
            debug(msg.strip(u'\n'))
        else:
            info('failed to send_request: not connected')

    def quote(msg):
        """Quote 'msg' and escape special characters."""
        return u'"%s"' % re_escape.sub(escape_char, msg)
    quote = staticmethod(quote)

    def split_quoted_string(msg):
        """Return the list of whitespace separated tokens from 'msg', handling
        double quoted substrings as a token.

         The '\' escaping character of the special characters in quoted
        substrings are removed.

        >>> print split_quoted_string(r'"a c" b v "this \\"is\\" foobar argument" Y ')
        ['a c', 'b', 'v', 'this "is" foobar argument', 'Y']

        """
        match = re_token_split.findall(msg)
        return [unquote(x) or y for x, y in match]
    split_quoted_string = staticmethod(split_quoted_string)

class NetbeansBuffer(object):
    """A Vim buffer.

    Instance attributes:
        pathname: readonly property
            full pathname
        registered: boolean
            True: buffer registered to Vim with netbeans
        lnum: int
            line number of the cursor (first line is one)
        col: int
            column number of the cursor (in bytes, zero based)
        buf_id: int
            netbeans buffer number, starting at one
        nbsock: netbeans.Netbeans
            the netbeans asynchat socket

    """

    def __init__(self, pathname, buf_id, nbsock):
        """Constructor."""
        self.__pathname = pathname
        self.registered = False
        self.lnum = 1
        self.col = 0
        self.buf_id = buf_id
        self.nbsock = nbsock

    def register(self, editFile=True):
        """Register the buffer with Netbeans."""
        if not self.registered:
            if editFile:
                self.nbsock.send_cmd(self, u'editFile',
                            self.nbsock.quote(self.pathname))
            self.nbsock.send_cmd(self, u'putBufferNumber',
                            self.nbsock.quote(self.pathname))
            self.nbsock.send_cmd(self, u'stopDocumentListen')
            self.registered = True

    def get_pathname(self):
        """NetbeansBuffer full path name."""
        return self.__pathname
    pathname = property(get_pathname, None, None, get_pathname.__doc__)

    def get_basename(self):
        """Return the basename of the buffer pathname."""
        return os.path.basename(self.pathname)

    def __str__(self):
        """Return the string representation of the buffer."""
        return '%s:%s:%s' % (self.get_basename(), self.lnum, self.col)

    __repr__ = __str__

class BufferSet(dict):
    """The Vim buffer set is a dictionary of {pathname: NetbeansBuffer instance}.

    Instance attributes:
        nbsock: netbeans.Netbeans
            the netbeans asynchat socket
        buf_list: python list
            the list of NetbeansBuffer instances indexed by netbeans 'bufID'

    A NetbeansBuffer instance is never removed from BufferSet.

    """

    def __init__(self, nbsock):
        """Constructor."""
        self.nbsock = nbsock
        self.buf_list = []

    def getbuf_at(self, buf_id):
        """Return the buffer at idx in list."""
        assert isinstance(buf_id, int)
        if buf_id <= 0 or buf_id > len(self.buf_list):
            return None
        return self.buf_list[buf_id - 1]

    #-----------------------------------------------------------------------
    #   Dictionary methods
    #-----------------------------------------------------------------------
    def __getitem__(self, pathname):
        """Get NetbeansBuffer with pathname as key, instantiate one when not found.

        The pathname parameter must be an absolute path name.

        """
        if not os.path.isabs(pathname):
            raise ValueError(
                '"pathname" is not an absolute path: %s' % pathname)
        if not pathname in self:
            # Netbeans buffer numbers start at one.
            buf = NetbeansBuffer(pathname, len(self.buf_list) + 1, self.nbsock)
            self.buf_list.append(buf)
            dict.__setitem__(self, pathname, buf)
        return dict.__getitem__(self, pathname)

    def __setitem__(self, pathname, item):
        """Mapped to __getitem__."""
        self.__getitem__(pathname)

    def setdefault(self, pathname, failobj=None):
        """Mapped to __getitem__."""
        return self.__getitem__(pathname)

    def __delitem__(self, key):
        """A key is never removed."""
        pass

    def popitem(self):
        """A key is never removed."""
        pass

    def pop(self, key, *args):
        """A key is never removed."""
        pass

    def update(self, dict=None, **kwargs):
        """Not implemented."""
        assert False, 'not implemented'

    def copy(self):
        """Not implemented."""
        assert False, 'not implemented'

def main():
    formatter = optparse.IndentedHelpFormatter(max_help_position=30)
    parser = optparse.OptionParser(
                    version='%prog ' + str(VERSION),
                    usage='usage: python %prog [options] args...',
                    formatter=formatter)
    parser.add_option('-d', '--debug',
            action="store_true", default=False,
            help='enable printing debug information')
    parser.add_option('--conf', type='string', default='.',
            help=('path to vimoir.properties directory (default \'%default\')'))
    (options, sys.argv) = parser.parse_args(args=sys.argv)
    setup_logger(options.debug)
    props_file = os.path.join(options.conf, 'vimoir.properties')
    opts = load_properties(props_file)

    Server(opts.get('vimoir.netbeans.host'), opts.get('vimoir.netbeans.port'),
                                                                    props_file)
    timeout = int(opts.get('vimoir.netbeans.timeout')) / 1000.0
    user_interval = (int(opts.get('vimoir.netbeans.user_interval')) / 1000.0)
    last = time.time()
    while asyncore.socket_map:
        asyncore.poll(timeout=timeout)

        # send the timer events
        now = time.time()
        if (now - last >= user_interval):
            last = now
            for obj in asyncore.socket_map.values():
                obj.handle_tick()

    # Terminate all Phonemic threads by exiting.
    sys.exit(0)

if __name__ == "__main__":
    if sys.version_info >= (3, 0):
        sys.stderr.write("Python 3 is not supported.\n")
        sys.exit(1)

    main()

