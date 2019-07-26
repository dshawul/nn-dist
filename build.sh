#!/bin/bash
rm -rf bin/*.class
javac -source 1.7 -target 1.7 -d bin src/*.java
