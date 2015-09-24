

# The vimoir project #
Vimoir is a server and a library for building Netbeans applications interfaced
with Vim and that includes an example allowing to hear (_oir_ in spanish) Vim.

Use the vimoir library to interface a java or a python application with Vim
through the Netbeans protocol. The client application implements the
`NetbeansEventHandler` interface which maps Vim Netbeans events to methods.
The vimoir library takes care of the details of the protocol such as registering
buffers with a `bufID`, managing `seqno` command sequence numbers,
instantiating the client class on each new connection request from a Vim
instance and managing the corresponding concurrent Netbeans socket connections.

Vimoir is hosted at [vimoir](http://code.google.com/p/vimoir/).


# Installation #
#### Java ####
The client class which is instantiated by the server on each connection request
from Vim is the class whose qualified name is defined by the
`vimoir.netbeans.java.client` property. The server loads its properties from
the `vimoir.properties` file. Add the directory containing this file to the
java classpath and to start the server, run the `vimoir.netbeans.Netbeans`
class located in `netbeans.jar`.

On start up the server invokes the `main()` function of the client class, if
this function exists, with the command line parameters as argument.

Vimoir uses the standard java `logging` API.

#### Python ####
The client class which is instantiated by the server on each connection request
from Vim is the class whose qualified name is defined by the
`vimoir.netbeans.python.client` property. The server loads its properties
from the `vimoir.properties` file and looks first for this file in the
directory indicated by the `--conf` command line parameter, and then in the
current directory.

Run the `netbeans.py` module to start the server. The module parses all the
options from the command line and removes them and `sys.argv[0]` from sys.argv.
The remaining command line parameters can be accessed by the client class in
`sys.argv`.

Use the `--debug` command line option when starting `netbeans.py` to
print the debugging information.

# The Phonemic client #
The vimoir project includes the `Phonemic` client as an example of a
NetbeansClientType. The Phonemic client uses the
[phonemic](http://sourceforge.net/apps/trac/phonemic/wiki) java library with
jython or java to make Vim speak the content of buffers or speak operational
messages such as Netbeans events.

You don't need to install phonemic to test the library: when phonemic.jar is not
available or cannot be used (such as when running with python), the Phonemic
client prints to stdout instead of speaking.

**Notes**:
  * Phonemic is a java cross-platform text-to-speech engine for Windows, Mac, and variants of Linux.
  * See also [some notes on installing phonemic on debian lenny](InstallPhonemicOnDebian.md).

#### Running the Phonemic client on linux ####
Follow these steps:
  * install [speech-dispatcher](http://devel.freebsoft.org/speechd)
  * install [phonemic](http://sourceforge.net/projects/phonemic/)
  * check that the phonemic `phonemicTestApp` application speaks messages
  * optionaly install jython
  * untar vimoir-v0.3.tar.gz
  * edit bin/vimoir.sh and set the following paths according to your setup:
    * JYTHON\_HOME (if needed)
    * PHONEMIC\_DIR
    * LINUXSPEAKJNI
  * run one of the following commands:
```

$ /path/to/bin/vimoir.sh [--debug] python # run Phonemic with python
$ /path/to/bin/vimoir.sh [--debug] jython # run Phonemic with jython
$ /path/to/bin/vimoir.sh java             # run Phonemic with java
```
  * run the `:nbstart` command in Vim to connect to the Phonemic client


# Development #
See the javadoc documentation at `doc/html/index.html` in the tarball
that may be downloaded from [Downloads](http://code.google.com/p/vimoir/downloads/list).

#### Building from source ####
Vimoir is an [ant](http://ant.apache.org/) project.

To build the jar files and the vimoir tarball, run:
```

$ ant -Dphonemic.dir=</path/to/phonemic_jar_dir>
```

The wiki repository should be located in the `vimoir.wiki` directory at the
root of the source code work area. The absence of this directory does not
prevent to build the project.

# Support #
Use the [issue tracker](http://code.google.com/p/vimoir/issues/list) to report
bugs, to request new features or to ask questions about vimoir. Issues about the
implementation of the Netbeans protocol in Vim should be submitted to
[the Vim development mailing list](mailto://vim-dev@vim.org).


# Licensing #
This software is licensed under the terms you may find in the file named
_LICENSE_ in this directory.

<a href='Hidden comment: 
vim:tw=80:sts=2:sw=2
'></a>