#!/bin/bash
rm -rf bin/*.class
javac -source 1.7 -target 1.7 -d bin -cp bin:bin/postgresql-42.2.6.jar src/*.java
