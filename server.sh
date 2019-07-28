#!/bin/bash
P=$( dirname ${BASH_SOURCE[0]} )
java -cp ${P}/bin:${P}/bin/postgresql-42.2.6.jar ConsoleInterface -debug -startServer
