#!/bin/bash

# display help
display_help() {
    echo "Usage: $0 [OS] [MACHINE] "
    echo
    echo "    OS          Operating system could be ubuntu/centos/windows/android."
    echo "  MACHINE       Hardware could be GPU or CPU"
    echo "  -h,--help     Display this help message."
    echo
    echo "Example: ./install.sh windows gpu"
    echo
}

if [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
  display_help
  exit 0
fi

set -eux

# Autodetect operating system
OSD=windows
if [[ "$OSTYPE" == "linux-gnu" ]]; then
  OSD=ubuntu
elif [[ "$OSTYPE" == "darwin"* ]]; then
  OSD=macosx
fi

# number of cores and gpus
CPUS=`grep -c ^processor /proc/cpuinfo`
if [ ! -z `which nvidia-smi` ]; then
    GPUS=`nvidia-smi --query-gpu=name --format=csv,noheader | wc -l`
    DEV=gpu
else
    GPUS=1
    DEV=cpu
fi

# Select
OS=${1:-$OSD}      # OS is either ubuntu/centos/windows/android
DEV=${2:-$DEV}     # Device is either gpu/cpu
VERSION=3.0        # Version of scorpio

# paths
VR=`echo $VERSION | tr -d '.'`
EGBB=nnprobe-${OS}-${DEV}
NET="nets-scorpio nets-lczero nets-maddex"
if [ $DEV = "gpu" ]; then
    nn_type=1
else
    nn_type=0
fi

# download
SCORPIO=Scorpio-train
mkdir -p $SCORPIO
cd $SCORPIO

# egbbdll & linnnprobe
LNK=https://github.com/dshawul/Scorpio/releases/download
wget --no-check-certificate ${LNK}/${VERSION}/${EGBB}.zip
unzip -o ${EGBB}.zip
# networks
for N in $NET; do
    wget --no-check-certificate ${LNK}/${VERSION}/$N.zip
    unzip -o $N.zip
done
# scorpio binary
wget --no-check-certificate ${LNK}/${VERSION}/scorpio${VR}-mcts-nn.zip
unzip -o scorpio${VR}-mcts-nn.zip

rm -rf *.zip
chmod 755 ${EGBB}
cd ${EGBB}
chmod 755 *
cd ../..

# number of threads
delay=0
if [ $DEV = "gpu" ]; then
    if [ $CPUS -le 4 ] || [ $GPUS -ge 2 ]; then
       delay=1
    fi
    if [ $CPUS -eq 1 ]; then
        mt=$((GPUS*64))
    else
        mt=$((GPUS*128))
    fi
else
    mt=$((CPUS*4))
    delay=1
fi

#paths
cd $SCORPIO
PD=`pwd`
PD=`echo $PD | sed 's/\/cygdrive//g'`
PD=`echo $PD | sed 's/\/c\//c:\//g'`
egbbp=${PD}/${EGBB}
if [ $nn_type -eq 1 ]; then
    nnp=${PD}/nets-maddex/net-maddex.uff
else
    nnp=${PD}/nets-scorpio/net-6x64.pb
fi
if [ $OS = "windows" ]; then
    exep=${PD}/bin/Windows
elif [ $OS = "android" ]; then
    exep=${PD}/bin/Android
else
    exep=${PD}/bin/Linux
fi
cd $exep

# Edit scorpio.ini
egbbp_=$(echo $egbbp | sed 's_/_\\/_g')
nnp_=$(echo $nnp | sed 's_/_\\/_g')
sed -i "s/^egbb_path.*/egbb_path                ${egbbp_}/g" scorpio.ini
sed -i "s/^nn_path.*/nn_path                  ${nnp_}/g" scorpio.ini
sed -i "s/^nn_type.*/nn_type                  ${nn_type}/g" scorpio.ini
sed -i "s/^float_type.*/float_type                  HALF/g" scorpio.ini
sed -i "s/^delay.*/delay                  ${delay}/g" scorpio.ini
if [ $DEV = "gpu" ]; then
    sed -i "s/^device_type.*/device_type              GPU/g" scorpio.ini
    sed -i "s/^n_devices.*/n_devices                ${GPUS}/g" scorpio.ini
    sed -i "s/^mt.*/mt                  ${mt}/g" scorpio.ini
else
    sed -i "s/^device_type.*/device_type              CPU/g" scorpio.ini
    sed -i "s/^n_devices.*/n_devices                1/g" scorpio.ini
    sed -i "s/^mt.*/mt                  ${mt}/g" scorpio.ini
fi

# Prepare script for scorpio and set PATH env variable
if [ $OS = "windows" ]; then
    EXE=scorpio.bat
else
    EXE=scorpio.sh
fi

cd ../..

# Test
echo "Running with delay 0"
$exep/$EXE delay 0 go quit
echo "Running with delay 1"
$exep/$EXE delay 1 go quit

