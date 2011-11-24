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
    jython $pwd/nbphonemic.py $pwd/phonemic/phonemic.jar "$@"
}

pgmname=${0##*/}
fullpath $0
pgm=$file
## goto base directory
cd ${pgm%/*}
pwd=$(pwd)
export LD_LIBRARY_PATH=$pwd/phonemic/libraries/linuxLibrary/LinuxSpeakJNI/dist/

run "$@"
