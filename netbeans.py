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
import asyncore
import asynchat
import socket
import cStringIO
import logging
import ConfigParser
from logging import error, info, debug

try:
    from vimoir.netbeans import NetbeansType
except ImportError:
    NetbeansType = object

PROPERTIES_PATHNAME = os.path.join('conf', 'vimoir.properties')
SECTION_NAME = 'vimoir properties'
DEFAULTS = {
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

# compile regexps
re_escape = re.compile(RE_ESCAPE, re.VERBOSE)
re_unescape = re.compile(RE_UNESCAPE, re.VERBOSE)
re_auth = re.compile(RE_AUTH, re.VERBOSE)
re_response = re.compile(RE_RESPONSE, re.VERBOSE)
re_event = re.compile(RE_EVENT, re.VERBOSE)
re_lnumcol = re.compile(RE_LNUMCOL, re.VERBOSE)

def escape_char(matchobj):
    """Escape special characters in string."""
    if matchobj.group(0) == u'"': return ur'\"'
    if matchobj.group(0) == u'\n': return ur'\n'
    if matchobj.group(0) == u'\t': return ur'\t'
    if matchobj.group(0) == u'\r': return ur'\r'
    if matchobj.group(0) == u'\\': return ur'\\'
    assert False

def quote(msg):
    """Quote 'msg' and escape special characters."""
    return u'"%s"' % re_escape.sub(escape_char, msg)

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

class Reply(object):
    """Abstract class. A Reply instance is a callable used to process
    the result of a  function call in the reply received from netbeans.

    Instance attributes:
        buf: Buffer
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
    def __init__(self, nbsock, host, port):
        asyncore.dispatcher.__init__(self)
        self.nbsock = nbsock
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
        conn.setblocking(0)
        self.nbsock.set_socket(conn)
        self.nbsock.connected = True
        self.close()
        info('connected to %s', addr)

    def handle_tick(self):
        pass

class Netbeans(asynchat.async_chat, NetbeansType):
    def __init__(self, debug):
        asynchat.async_chat.__init__(self)
        self.client = None
        self.set_terminator(u'\n')
        self.ibuff = []
        self.ready = False
        self.reply_fifo = asynchat.fifo()
        self._bset = BufferSet(self)
        self.seqno = 0
        self.last_seqno = 0
        setup_logger(debug)
        self.opts = RawConfigParser(DEFAULTS)
        try:
            self.opts.readfp(FileLike(PROPERTIES_PATHNAME))
        except IOError:
            self.opts.add_section(SECTION_NAME)
        except ConfigParser.ParsingError, e:
            error(e)
        self.encoding = self.opts.get('vimoir.netbeans.encoding')
        Server(self, self.opts.get('vimoir.netbeans.host'),
                        self.opts.get('vimoir.netbeans.port'))

    def start(self, client):
        self.client = client
        timeout = int(self.opts.get('vimoir.netbeans.timeout')) / 1000.0
        user_interval = (int(self.opts.get('vimoir.netbeans.user_interval'))
                                            / 1000.0)
        last = time.time()
        while asyncore.socket_map:
            asyncore.poll(timeout=timeout)

            # send the timer events
            now = time.time()
            if (now - last >= user_interval):
                last = now
                for obj in asyncore.socket_map.values():
                    obj.handle_tick()

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
            if (matchobj.group(u'passwd')
                    == self.opts.get('vimoir.netbeans.password')):
                return
            else:
                raise Error('invalid password: "%s"' % self.passwd)
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
                        self.send_cmd(buf, u'putBufferNumber',
                                                quote(pathname))
                        self.send_cmd(buf, u'stopDocumentListen')
                        buf.registered = True
                        buf.update()
                    else:
                        error('got fileOpened with wrong bufId')
                        return
                self.client.event_fileOpened(pathname)
            else:
                error('absolute pathname required')
        else:
            self.client.event_error(
                u'You cannot use netbeans on a "[No Name]" file.\n'
                u'Please, edit a file.'
                )

    def evt_keyAtPos(self, buf_id, nbstring, arg_list):
        """Process a keyAtPos netbeans event."""
        buf = self._bset.getbuf(buf_id)
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
                lnum = int(matchobj.group(u'lnum'))
                col = int(matchobj.group(u'col'))
                cmd, args = (lambda a=u'', b=u'':
                                    (a, b))(*nbstring.split(None, 1))
                try:
                    method = getattr(self.client, 'cmd_%s' % cmd)
                    method(args, buf.name, lnum, col)
                except AttributeError:
                    self.client.default_cmd_processing(
                                    cmd, args, buf.name, lnum, col)

    def evt_killed(self, buf_id, nbstring, arg_list):
        """A file was closed by the user."""
        buf = self._bset.getbuf(buf_id)
        if buf is None:
            error('invalid bufId: "%s" in killed', buf_id)
        else:
            buf.registered = False
            self.client.event_killed(buf.name)

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
        if isinstance(buf, Buffer):
            buf_id = buf.buf_id
        if not args:
            space = u''
        msg = fmt % (buf_id, request, self.seqno, space, args)
        self.push(msg.encode(self.encoding))
        debug(msg.strip(u'\n'))

class Buffer(dict):
    """A Vim buffer.

    Instance attributes:
        name: readonly property
            full pathname
        buf_id: int
            netbeans buffer number, starting at one
        nbsock: netbeans.Netbeans
            the netbeans asynchat socket
        registered: boolean
            True: buffer registered to Vim with netbeans

    """

    def __init__(self, name, buf_id, nbsock):
        """Constructor."""
        self.__name = name
        self.buf_id = buf_id
        self.nbsock = nbsock
        self.registered = False

    def update(self, anno_id=None, disabled=False):
        """Update the buffer with netbeans."""
        # Register file with netbeans.
        if not self.registered:
            self.nbsock.send_cmd(self, u'editFile', misc.quote(self.name))
            self.nbsock.send_cmd(self, u'putBufferNumber', misc.quote(self.name))
            self.nbsock.send_cmd(self, u'stopDocumentListen')
            self.registered = True

    def getname(self):
        """Buffer full path name."""
        return self.__name
    name = property(getname, None, None, getname.__doc__)

class BufferSet(dict):
    """The Vim buffer set is a dictionary of {pathname: Buffer instance}.

    Instance attributes:
        nbsock: netbeans.Netbeans
            the netbeans asynchat socket
        buf_list: python list
            the list of Buffer instances indexed by netbeans 'bufID'

    A Buffer instance is never removed from BufferSet.

    """

    def __init__(self, nbsock):
        """Constructor."""
        self.nbsock = nbsock
        self.buf_list = []

    def getbuf(self, buf_id):
        """Return the Buffer at idx in list."""
        assert isinstance(buf_id, int)
        if buf_id <= 0 or buf_id > len(self.buf_list):
            return None
        return self.buf_list[buf_id - 1]

    #-----------------------------------------------------------------------
    #   Dictionary methods
    #-----------------------------------------------------------------------
    def __getitem__(self, pathname):
        """Get Buffer with pathname as key, instantiate one when not found.

        The pathname parameter must be an absolute path name.

        """
        if not os.path.isabs(pathname):
            raise ValueError(
                '"pathname" is not an absolute path: %s' % pathname)
        if not pathname in self:
            # Netbeans buffer numbers start at one.
            buf = Buffer(pathname, len(self.buf_list) + 1, self.nbsock)
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

