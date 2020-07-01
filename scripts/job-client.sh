#!/bin/bash

set -e

#setup parameters for selfplay
SC=./Scorpio/bin/Linux         # workding directory of engine
EXE=scorpio.sh                 # engine executable
G=512                          # games per worker
SV=$1                          # mcts simulations
CPUCT=$2                       # Cpuct constant
POL_TEMP=$3                    # Policy temeprature
NOISE_FRAC=$4                  # Fraction of Dirchilet noise
HEAD_TYPE=$5                   # NN heads
RAND_TEMP=$6                   # Temperature for random selection
NOISE_ALPHA=$7                 # Alpha parameter
NOISE_BETA=$8                  # Beta parameter
FORCED_PLAYOUTS=$9             # Use forced playouts
POLICY_PRUNING=${10}           # Use policy pruning
FPU_IS_LOSS=${11}              # FPU is loss, win, or reduction
FPU_RED=${12}                  # FPU is reduction

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
    SCOPT="reuse_tree 0 backup_type 6 alphabeta_man_c 0 min_policy_value 0 \
           train_data_type ${HEAD_TYPE} fpu_is_loss ${FPU_IS_LOSS} fpu_red ${FPU_RED} cpuct_init ${CPUCT} \
           rand_temp ${RAND_TEMP} policy_temp ${POL_TEMP} noise_frac ${NOISE_FRAC} \
           noise_alpha ${NOISE_ALPHA} noise_beta ${NOISE_BETA} forced_playouts ${FORCED_PLAYOUTS} \
           policy_pruning ${POLICY_PRUNING}"
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
