#!/bin/bash

trap 'kill $(jobs -p)' EXIT INT

if [ ! -L Scorpio ]; then
  scripts/install.sh --no-egbb --no-lcnets --factor 1
fi

exec java -cp bin ConsoleInterface -debug -startClient
