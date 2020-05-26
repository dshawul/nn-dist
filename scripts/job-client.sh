#!/bin/bash

set -e

#setup parameters for selfplay
SC=./Scorpio/bin/Linux         # workding directory of engine
EXE=scorpio.sh                 # engine executable
G=1024                         # games per worker
SV=$1                          # mcts simulations
CPUCT=$2                       # Cpuct constant
POL_TEMP=$3                    # Policy temeprature
NOISE_FRAC=$4                  # Fraction of Dirchilet noise
HEAD_TYPE=$5                   # NN heads

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
    SCOPT="train_data_type ${HEAD_TYPE} alphabeta_man_c 0 min_policy_value 0 \
           reuse_tree 0 fpu_is_loss 0 fpu_red 0 cpuct_init ${CPUCT} \
           backup_type 6 policy_temp ${POL_TEMP} noise_frac ${NOISE_FRAC}"
    ALLOPT="nn_type 0 nn_path ${NDIR} new ${SCOPT} sv ${SV} \
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
