import java
import sys
import os

import phonemic
import netbeans

def usage():
    print >> sys.stderr, ('usage: %s /path/to/phonemic.jar'
                            % os.path.basename(sys.argv[0]))
    sys.exit(1)

def get_speech():
    try:
        sys.path.append(sys.argv[1])
        import org.sodbeans.phonemic.TextToSpeechFactory as TextToSpeechFactory
    except IndexError:
        usage()
    except java.lang.UnsatisfiedLinkError:
        pass
    except ImportError, err:
        print >> sys.stderr, 'cannot find phonemic.jar: %s' % str(err)
        pass
    else:
        speech = TextToSpeechFactory.getDefaultTextToSpeech()
        if speech.canSetSpeed():
            print >> sys.stderr, 'speech speed: %f' % speech.getSpeed()
            pass
        return speech
    sys.exit(1)

def main():
    nbsock = phonemic.Phonemic(get_speech())
    nbserver = netbeans.Server(nbsock, debug=1)
    nbserver.bind_listen()
    nbserver.loop()
    print >> sys.stderr, 'Terminated.'

    # terminate all Phonemic threads by exiting
    sys.exit(0)

if __name__ == "__main__":
    main()

