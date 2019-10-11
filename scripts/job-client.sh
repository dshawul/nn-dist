#!/bin/bash

set -e

#setup parameters for selfplay
SC=./Scorpio-train/bin/Linux   # workding directory of engine
EXE=scorpio.sh                 # engine executable
G=256                          # games per worker
SV=$1                          # mcts simulations
CPUCT=$2                       # Cpuct constant
POL_TEMP=$3                    # Policy temeprature
NOISE_FRAC=$4                  # Fraction of Dirchilet noise

#check if Scorpio directory exists
if [ ! -f ${SC}/${EXE} ]; then
    exit 0
fi

#number of cpus and gpus
CPUS=`grep -c ^processor /proc/cpuinfo`
if [ ! -z `which nvidia-smi` ]; then
    GPUS=`nvidia-smi --query-gpu=name --format=csv,noheader | wc -l`
    NDIR=$PWD/net.uff
    rm -rf *.trt
else
    GPUS=0
    NDIR=$PWD/net.pb
fi

#run selfplay
run() {
    export CUDA_VISIBLE_DEVICES="$1" 
    SCOPT="reuse_tree 0 fpu_is_loss 0 fpu_red 0 cpuct_init ${CPUCT} \
           backup_type 6 policy_temp ${POL_TEMP} noise_frac ${NOISE_FRAC}"
    taskset -c $3 ./${EXE} nn_type 0 nn_path ${NDIR} new ${SCOPT} \
            sv ${SV} pvstyle 1 selfplayp $2 games$1.pgn train$1.epd quit
}

#use all gpus
rungames() {
    if [ $GPUS -le 0 ]; then
	run 0 $1 0-$((CPUS-1)):1 &
    else
        I=$((CPUS/GPUS))
        for k in `seq 0 $((GPUS-1))`; do
            run $k $1 $((k*I))-$((k*I+I-1)):1 &
        done
    fi
    wait
    echo "All jobs finished"
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
