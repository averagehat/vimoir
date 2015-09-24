

The installation of phonemic on amd64 debian lenny is not straightforward. Here
is a recipe:

# Install festival #
```

# aptitude install festival festival-freebsoft-utils sound-icons
```
Run festival and check that it can speak messages:
```

$ festival
festival> (SayText "Hello")
```


# Install speech-dispatcher 0.7.1 #
We need to install speech-dispatcher from source because the debian
lenny version is too old.
```

# aptitude install speech-dispatcher-festival libdotconf-dev
```

Download and expand the tarball from
http://devel.freebsoft.org/pub/projects/speechd/speech-dispatcher-0.7.1.tar.gz

Build speech-dispatcher:
```

$ ./configure; make all
# make install
# ldconfig
```

There is a bug in 0.7.1 that will be fixed in the next version: when
python is not installed at /usr/bin and spd-conf fails to run, replace
the first line in /usr/local/bin/spd-conf with:
`#! /usr/bin/env python:`

Run and test speech-dispatcher as a plain user:
```

$ festival --server
$ spd-conf
...
Configuring user settings for Speech Dispatcher
Default output module [espeak] :
>festival
Default language (two-letter iso language code like "en" or "cs") [en] :
>
Default audio output method [pulse] :
>alsa
...
$ spd-say "Hello"
```

The logs can be found at: `~/.speech-dispatcher/log`


# Install phonemic #
Download and expand the zip file from
http://sourceforge.net/projects/phonemic/files/latest/download

Test phonemic (but on 64 bits linux, first compile the jni library,
see below):
```

$ cd /path/to/phonemic/phonemic/example/phonemicTestApp/dist
$ java -Djava.library.path="../jni" -jar phonemicTestApp.jar "Hello there"
```

Compile the jni library libLinuxSpeakJNI.so (not needed on 32 bits linux):
  * check out the phonemic source:
```

$ svn co https://phonemic.svn.sourceforge.net/svnroot/phonemic phonemic
$ cd /path/to//trunk/libraries/linuxLibrary/LinuxSpeakJNI
```
  * edit nbproject/Makefile-Debug.mk with vim:
```

:" <JAVA_HOME> is the path to where java is installed on your system
:%s/-I\/usr\/lib\/jvm\/java-6-openjdk\/include\//<JAVA_HOME>/
```
  * build the library:
```

$ make
```


<a href='Hidden comment: 
vim:tw=80:sts=2:sw=2
'></a>