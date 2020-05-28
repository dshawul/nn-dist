#!/bin/bash

trap 'kill $(jobs -p)' EXIT INT

if [ -z `which nvidia-smi` ]; then
   echo "No GPUs detected. Please try again when you have one. Thanks!."
   exit 0
fi

if [ ! -L Scorpio ]; then
  scripts/install.sh --no-egbb --no-lcnets --precision INT8
fi

exec java -cp bin ConsoleInterface -debug -startClient
