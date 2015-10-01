#!/usr/bin/python

from settings import *

# Client config
benchClientClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchClient"
clientId = 1

# Run client
#export MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n"
clientCommand = "mvn "+ mvn_options + " exec:java -Dexec.mainClass=%s -Dexec.args=\"%s %s %s %s %s %s %s\" " % ( benchClientClass, \
    clientId, configPath, messageSize, numPermits, gathererNode, gathererPort, benchDuration)

localcmd(clientCommand)
