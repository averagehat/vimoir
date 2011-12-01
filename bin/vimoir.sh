#! /bin/sh
SRC=/home/xavier/src/packages
JYTHON_HOME=$SRC/jython/jython2.5.2
PHONEMIC_DIR=$SRC/phonemic/phonemic-unziped/phonemic/phonemic
LINUXSPEAKJNI=/home/xavier/src/vimoir/phonemic-src/libraries/linuxLibrary/LinuxSpeakJNI/dist/

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
        export LD_LIBRARY_PATH=$LINUXSPEAKJNI
        jython $pwd/vimoir.py $PHONEMIC_DIR/phonemic.jar
    elif [[ "$1" = "python" ]] ; then
        python $pwd/vimoir.py
    else
        export LD_LIBRARY_PATH=$LINUXSPEAKJNI
        jarfiles=$JYTHON_HOME/jython.jar
        jarfiles=$jarfiles:$PHONEMIC_DIR/phonemic.jar
        jarfiles=$jarfiles:$pwd/lib/jynetbeans-v0.1.jar
        java -cp $jarfiles vimoir.jynetbeans.Phonemic $JYTHON_HOME/lib
    fi

}

pgmname=${0##*/}
fullpath $0
pgm=$file
## goto base directory
cd ${0%/*}/../
pwd=$(pwd)

run "$@"
