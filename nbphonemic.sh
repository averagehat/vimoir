#! /bin/sh

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
    # path to jython directory as first parameter: run as java
    # otherwise run as jython
    if [[ -n "$1" ]] ; then
        java -cp $pwd/phonemic.jar:$1/jython.jar:$pwd/dist/factory.jar \
                 nbphonemic.factory.Main $1/lib
    else
        jython $pwd/nbphonemic.py $pwd/phonemic.jar
    fi

}

pgmname=${0##*/}
fullpath $0
pgm=$file
## goto base directory
cd ${pgm%/*}
pwd=$(pwd)
export LD_LIBRARY_PATH=$pwd/phonemic-src/libraries/linuxLibrary/LinuxSpeakJNI/dist/

run "$@"
