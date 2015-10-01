#!/usr/bin/python

from settings import *

# Server Config
benchServerClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchServer"
serverId = 1

# Run server
serverCommand = "mvn "+ mvn_options + " exec:java -Dexec.mainClass=%s -Dexec.args=\"%s %s %s %s %s %s\" " % ( benchServerClass, \
    serverId, configPath, gathererNode, gathererPort, logdir, benchDuration)

localcmd(serverCommand)
