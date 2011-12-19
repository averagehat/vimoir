#! /bin/bash
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


# Set the paths of the following variables according to your own setup.
JYTHON_HOME=$SRC/jython/jython2.5.2
PHONEMIC_DIR=$SRC/phonemic/phonemic/phonemic
LINUXSPEAKJNI=$SRC/phonemic/phonemic-svn/trunk/libraries/linuxLibrary/LinuxSpeakJNI/dist

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
    args=("$@")

    if [[ "${!#}" = "python" ]] ; then
        unset args[${#args[@]}-1]
        python $pwd/netbeans.py --conf=conf "${args[@]}"

    elif [[ "${!#}" = "jython" ]] ; then
        unset args[${#args[@]}-1]
        export LD_LIBRARY_PATH=$LINUXSPEAKJNI
        # mandatory parameter: path to phonemic.jar file
        jython $pwd/netbeans.py --conf=conf "${args[@]}"            \
                                $PHONEMIC_DIR/phonemic.jar

    elif [[ "${!#}" = "java" ]] ; then
        export LD_LIBRARY_PATH=$LINUXSPEAKJNI
        jarfiles=$jarfiles:$PHONEMIC_DIR/phonemic.jar
        jarfiles=$jarfiles:$pwd/lib/netbeans.jar
        jarfiles=$jarfiles:$pwd/lib/netbeans-examples.jar
        java -cp $jarfiles:conf                                     \
            -Djava.util.logging.config.file=conf/logging.properties \
            vimoir.netbeans.Netbeans

    else
        echo "usage: $pgmname python|jython|java"
    fi

}

pgmname=${0##*/}
fullpath $0
pgm=$file
## goto base directory
cd ${0%/*}/../
pwd=$(pwd)

run "$@"
