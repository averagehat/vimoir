import java
import sys
import os

if len(sys.argv) == 1:
    print 'Usage: %s /path/to/phonemic.jar' % os.path.basename(sys.argv[0])
    sys.exit(1)
sys.path.append(sys.argv[1])

try:
    import org.sodbeans.phonemic.TextToSpeechFactory as TextToSpeechFactory
except java.lang.UnsatisfiedLinkError:
    sys.exit(1)
except ImportError, err:
    print 'Cannot find phonemic.jar: %s' % err
    sys.exit(1)

def main():
    speech = TextToSpeechFactory.getDefaultTextToSpeech()
    speech.speakBlocking('Hello the sky is blue '
                         'and the sea has been yellow for a while.')
    sys.exit(0)

if __name__ == "__main__":
    main()

