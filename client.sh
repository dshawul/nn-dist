#!/bin/bash

trap 'kill $(jobs -p)' EXIT INT

if [ ! -L Scorpio ]; then
  scripts/install.sh --no-egbb --no-lcnets --precision INT8
fi

exec java -cp bin ConsoleInterface -debug -startClient
