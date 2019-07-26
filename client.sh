#!/bin/bash

trap 'kill $(jobs -p)' EXIT

if [ ! -d Scorpio-train ]; then
  scripts/install.sh
fi

exec java -cp bin ConsoleInterface -debug -startClient
