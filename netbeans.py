import sys
import os
import re
import asyncore
import asynchat
import socket
import cStringIO
import logging
from logging import error, info, debug

try:
    from nbphonemic.factory import ServerType
except ImportError:
    ServerType = object

CONNECTION_DEFAULT = ('', 3219, 'changeme')
RE_ESCAPE = r'["\n\t\r\\]'                                              \
            r'# RE: escaped characters in a string'
RE_UNESCAPE = r'\\["ntr\\]'                                             \
              r'# RE: escaped characters in a quoted string'
RE_AUTH = r'^\s*AUTH\s*(?P<passwd>\S+)\s*$'                             \
          r'# RE: password authentication'
RE_RESPONSE = r'^\s*(?P<seqno>\d+)\s*(?P<args>.*)\s*$'                  \
              r'# RE: a netbeans response'
RE_EVENT = r'^\s*(?P<buf_id>\d+):(?P<event>\S+)=(?P<seqno>\d+)'         \
           r'\s*(?P<args>.*)\s*$'                                       \
           r'# RE: a netbeans event message'
RE_LNUMCOL = r'^(?P<lnum>\d+)/(?P<col>\d+)'                             \
             r'# RE: lnum/col'

# compile regexps
re_escape = re.compile(RE_ESCAPE, re.VERBOSE)
re_unescape = re.compile(RE_UNESCAPE, re.VERBOSE)
re_auth = re.compile(RE_AUTH, re.VERBOSE)
re_response = re.compile(RE_RESPONSE, re.VERBOSE)
re_event = re.compile(RE_EVENT, re.VERBOSE)
re_lnumcol = re.compile(RE_LNUMCOL, re.VERBOSE)

def escape_char(matchobj):
    """Escape special characters in string."""
    if matchobj.group(0) == '"': return r'\"'
    if matchobj.group(0) == '\n': return r'\n'
    if matchobj.group(0) == '\t': return r'\t'
    if matchobj.group(0) == '\r': return r'\r'
    if matchobj.group(0) == '\\': return r'\\'
    assert False

def quote(msg):
    """Quote 'msg' and escape special characters."""
    return '"%s"' % re_escape.sub(escape_char, msg)

def unescape_char(matchobj):
    """Remove escape on special characters in quoted string."""
    if matchobj.group(0) == r'\"': return '"'
    if matchobj.group(0) == r'\n': return '\n'
    if matchobj.group(0) == r'\t': return '\t'
    if matchobj.group(0) == r'\r': return '\r'
    if matchobj.group(0) == r'\\': return '\\'
    assert False

def unquote(msg):
    """Remove escapes from escaped characters in a quoted string."""
    return '%s' % re_unescape.sub(unescape_char, msg)

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
        bufid_name = matchobj.group('buf_id')
        event = matchobj.group('event')
    else:
        # a reply
        bufid_name = '0'
        event = ''
        matchobj = re_response.match(msg)
    if not matchobj:
        error('discarding invalid netbeans message: "%s"', msg)
        return (None,)

    seqno = matchobj.group('seqno')
    args = matchobj.group('args').strip()
    try:
        buf_id = int(bufid_name)
        seqno = int(seqno)
    except ValueError:
        assert False, 'error in regexp'

    # a netbeans string
    nbstring = ''
    if args and args[0] == "\"":
        end = args.rfind("\"")
        if end != -1 and end != 0:
            nbstring = args[1:end]
            # do not unquote nbkey parameter twice since vim already parses
            # function parameters as strings (see :help expr-quote)
            if event != 'keyAtPos':
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

class Reply:
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

class Server(asyncore.dispatcher, ServerType):
    def __init__(self, nbsock, debug):
        asyncore.dispatcher.__init__(self)
        self.nbsock = nbsock
        setup_logger(debug)
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)

    def bind_listen(self):
        self.set_reuse_addr()
        self.bind(CONNECTION_DEFAULT[:2])
        self.listen(1)
        info('listening on: %s', CONNECTION_DEFAULT[:2])

    def handle_accept(self):
        """Accept the connection from Vim."""
        conn, addr = self.socket.accept()
        conn.setblocking(0)
        self.nbsock.set_socket(conn)
        self.nbsock.connected = True
        self.close()
        info('connected to %s', addr)

    def loop(self):
        asyncore.loop(timeout=.020, use_poll=False)

class Netbeans(asynchat.async_chat):
    def __init__(self):
        asynchat.async_chat.__init__(self)
        self.set_terminator('\n')
        self.ibuff = []
        self.ready = False
        self.reply_fifo = asynchat.fifo()
        self._bset = BufferSet(self)
        self.seqno = 0
        self.last_seqno = 0

    def collect_incoming_data(self, data):
        self.ibuff.append(data)

    def found_terminator(self):
        """Process new line terminated netbeans message."""
        msg = ''.join(self.ibuff)
        self.ibuff = []
        debug(msg)

        if not self.ready:
            self.open_session(msg)
            return

        # handle variable number of elements in returned tuple
        is_event, buf_id, event, seqno, nbstring, arg_list = (
                (lambda a, b=None, c=None, d=None, e=None, f=None:
                            (a, b, c, d, e, f))(*parse_msg(msg)))

        if is_event is None:
            # ignore invalid message
            pass
        elif is_event:
            evt_handler = getattr(self, "evt_%s" % event, evt_ignore)
            evt_handler(buf_id, nbstring, arg_list)

        # a function reply: process the reply
        else:
            # vim may send multiple replies for one function request
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
            if matchobj.group('passwd') == CONNECTION_DEFAULT[2]:
                return
            else:
                raise Error('invalid password: "%s"' % self.passwd)
        # '0:version=0 "2.3"'
        # '0:startupDone=0'
        else:
            # handle variable number of elements in returned tuple
            is_event, buf_id, event, seqno, nbstring, arg_list = (
                    (lambda a, b=None, c=None, d=None, e=None, f=None:
                                (a, b, c, d, e, f))(*parse_msg(msg)))

            if is_event:
                if event == "version":
                    return
                elif event == "startupDone":
                    self.ready = True
                    self.event_open()
                    return
        raise Error('received unexpected message: "%s"' % msg)

    #-----------------------------------------------------------------------
    #   Events
    #-----------------------------------------------------------------------
    def evt_disconnect(self, buf_id, nbstring, arg_list):
        """Process a disconnect netbeans event."""
        self.event_close()
        self.close()

    def evt_fileOpened(self, buf_id, pathname, arg_list):
        """A file was opened by the user."""
        if pathname:
            if os.path.isabs(pathname):
                buf = self._bset[pathname]
                if buf.buf_id != buf_id:
                    if buf_id == 0:
                        self.send_cmd(buf, 'putBufferNumber',
                                                quote(pathname))
                        self.send_cmd(buf, 'stopDocumentListen')
                        buf.registered = True
                        buf.update()
                    else:
                        error('got fileOpened with wrong bufId')
                        return
                self.event_fileOpened(pathname)
            else:
                error('absolute pathname required')
        else:
            self.show_balloon(
                '\nYou cannot use netbeans on a "[No Name]" file.\n'
                'Please, edit a file.\n'
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
                lnum = int(matchobj.group('lnum'))
                col = int(matchobj.group('col'))
                cmd, args = (lambda a='', b='':
                                    (a, b))(*nbstring.split(None, 1))
                try:
                    method = getattr(self, 'cmd_%s' % cmd)
                    method(args, buf, lnum, col)
                except AttributeError:
                    self.default_cmd_processing(cmd, args, buf, lnum, col)

    def evt_killed(self, buf_id, nbstring, arg_list):
        """A file was closed by the user."""
        buf = self._bset.getbuf(buf_id)
        if buf is None:
            error('invalid bufId: "%s" in killed', buf_id)
        else:
            buf.registered = False
            self.event_killed(buf.name)

    #-----------------------------------------------------------------------
    #   Commands - Functions
    #-----------------------------------------------------------------------

    def show_balloon(self, text):
        """Show the Vim balloon."""
        # restrict size to 2000 chars, about...
        size = 2000
        if len(text) > size:
            size //= 2
            text = text[:size] + '...' + text[-size:]
        self.send_cmd(None, 'showBalloon', quote(text))

    def send_cmd(self, buf, cmd, args=''):
        """Send a command to Vim."""
        self.send_request('%d:%s!%d%s%s\n', buf, cmd, args)

    def send_function(self, buf, function, args=''):
        """Send a function call to Vim."""
        try:
            clss = eval('%sReply' % function)
        except NameError:
            assert False, 'internal error, no reply class for %s' % function
        assert issubclass(clss, Reply)
        reply = clss(buf, self.seqno + 1, self)
        self.reply_fifo.push(reply)
        self.send_request('%d:%s/%d%s%s\n', buf, function, args)

    def send_request(self, fmt, buf, request, args):
        """Send a netbeans function or command."""
        self.seqno += 1
        buf_id = 0
        space = ' '
        if isinstance(buf, Buffer):
            buf_id = buf.buf_id
        if not args:
            space = ''
        msg = fmt % (buf_id, request, self.seqno, space, args)
        self.push(msg)
        debug(msg.strip('\n'))

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
        # open file in netbeans
        if not self.registered:
            self.nbsock.send_cmd(self, 'editFile', misc.quote(self.name))
            self.nbsock.send_cmd(self, 'putBufferNumber', misc.quote(self.name))
            self.nbsock.send_cmd(self, 'stopDocumentListen')
            self.registered = True

    # readonly property
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
            # netbeans buffer numbers start at one
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

