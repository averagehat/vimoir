#! /bin/sh
SRC=/home/xavier/src/packages
JYTHON_HOME=$SRC/jython/jython2.5.2
PHONEMIC_DIR=$SRC/phonemic/phonemic-unziped/phonemic/phonemic
LINUXSPEAKJNI=./phonemic-src/libraries/linuxLibrary/LinuxSpeakJNI/dist/

# convert to full pathname
fullpath()
{
    local dir=$(dirname $1)
    dir=$(cd "$dir" 2> /dev/null && pwd -P)
    if [[ -n "$dir" ]] ; then
        file="$dir/${1##*/}"
    fi
}

run()
{
    if [[ "$1" = "jython" ]] ; then
        jython $pwd/vimoir.py $PHONEMIC_DIR/phonemic.jar
    elif [[ "$1" = "python" ]] ; then
        python $pwd/vimoir.py
    else
        jarfiles=$JYTHON_HOME/jython.jar
        jarfiles=$jarfiles:$PHONEMIC_DIR/phonemic.jar
        jarfiles=$jarfiles:$pwd/dist/jynetbeans.jar
        java -cp $jarfiles vimoir.jynetbeans.Phonemic $JYTHON_HOME/lib
    fi

}

pgmname=${0##*/}
fullpath $0
pgm=$file
## goto base directory
cd ${pgm%/*}
pwd=$(pwd)
export LD_LIBRARY_PATH=$LINUXSPEAKJNI

run "$@"
