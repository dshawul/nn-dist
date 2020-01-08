#!/bin/bash

trap 'kill $(jobs -p)' EXIT INT

if [ ! -L Scorpio ]; then
  scripts/install.sh
fi

exec java -cp bin ConsoleInterface -debug -startClient
