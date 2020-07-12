#!/bin/bash

set -e

#setup parameters for selfplay
SC=./Scorpio/bin/Linux         # workding directory of engine
EXE=scorpio.sh                 # engine executable
G=512                          # games per worker
SCOPT="$@"                     # all options

#launch multiple jobs with mpi
RANKS=1
if [ $RANKS -gt 1 ]; then
   MPICMD="mpirun -np ${RANKS}"
else
   MPICMD=
fi

#check if Scorpio directory exists
if [ ! -f ${SC}/${EXE} ]; then
    exit 0
fi

#number of cpus and gpus
if [ ! -z `which nvidia-smi` ]; then
    NDIR=$PWD/net.uff
else
    NDIR=$PWD/net.pb
fi

#run selfplay
rungames() {
    ALLOPT="nn_type 0 nn_path ${NDIR} new ${SCOPT} \
	   pvstyle 1 selfplayp ${G} games.pgn train.epd quit"
    time ${MPICMD} ./${EXE} ${ALLOPT}
}

#get selfplay games
get_selfplay_games() {
    rm -rf cgames.pgn ctrain.epd
    cd ${SC}
    rungames ${G}
    cat games*.pgn* > cgames.pgn
    cat train*.epd* > ctrain.epd
    rm -rf games*.pgn* train*.epd*
    cd - > /dev/null 2>&1
    mv ${SC}/cgames.pgn .
    mv ${SC}/ctrain.epd .
}

get_selfplay_games
