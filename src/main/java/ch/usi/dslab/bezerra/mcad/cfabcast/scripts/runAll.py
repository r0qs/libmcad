#!/usr/bin/python
# -*- coding: utf-8 -*-

import settings
import sys, os
from os.path import join, expanduser
from time import sleep

javaCommand = "java -XX:+UseG1GC -Xms3g -Xmx3g"

# Directories
SRC = join(expanduser('~'), "test")
JARS = join(SRC, "jars")
logdir = SRC + "/logsmcast"

# Jars
sensejar = join(JARS, "libsense-git.jar")
netwrapperjar = join(JARS, "libnetwrapper-git.jar")
ridgejar = join(JARS, "ridge-git.jar")
cfabcastjar = join(JARS, "CFABCast-assembly-0.1-SNAPSHOT.jar")
libmcadjar = join(JARS, "libmcad-git-allinone.jar")

# cleanup logs
#clean_log(logdir)

lastNode = settings.availableNodes[len(settings.availableNodes)-1:]
remainingNodes = settings.availableNodes[:len(settings.availableNodes)-1]
serviceNodes = remainingNodes
serverNodes = remainingNodes
clientsNodes = remainingNodes
monitorNode = lastNode[0]

# MONITORING
gathererNode = monitorNode
javaGathererClass = "ch.usi.dslab.bezerra.sense.DataGatherer"
javaDynamicGathererClass = "ch.usi.dslab.bezerra.mcad.benchmarks.DynamicBenchGatherer"
gathererPort = "60000"

# Global config
configPath = SRC + "/config_parameters.json"
# número de requisições que o cliente pode fazer por vez antes de receber alguma resposta
numPermits = 10
# tamanho da msg em bytes
messageSize = 200
# duração em segundos
benchDuration = 60
# numero de clientes (BenchClient)
numClients = 3 
# numero de grupos de multicast
numGroups = 1
# Uma maneira de aumentar throughput é ter vários grupos de acceptors independentes gerando mensagens, e os learners fazem merge determinístico das streams de mensagens. Isso divide a carga de ordenação entre conjuntos de processos paxos independentes, mas aumenta o processamento dos learners e, potencialmente, aumenta a latência se os conjuntos de paxos não estiverem sincronizados.
numPxPerGroup = 1
# cada learner tem um BenchServer associado, cria-se numLearners learners, e daí eles são divididos entre os numGroups grupos de multicast
numLearners = 3

sshcmd = settings.sshcmd
sshcmdbg = settings.sshcmdbg
localcmd = settings.localcmd

print "Starting service nodes..."
for node in serviceNodes:
    javaservicecmd = "%s -jar %s %s" % (javaCommand, cfabcastjar, node)
    sshcmdbg(node, javaservicecmd)
    sleep(2)

# Server Config
# Run server
##serverCommand = "mvn "+ mvn_options + " exec:java -Dexec.mainClass=%s -Dexec.args=\"%s %s %s %s %s %s\" " % ( benchServerClass, serverId, configPath, gathererNode, gathererPort, logdir, benchDuration)
benchServerClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchServer"
for node in settings.createIdPerNodeList(serverNodes):
    env = "export APP_HOST=\"%s\" APP_PORT=%s ;" % (node["host"], 2550)
    serverCommand = "%s -cp %s %s %s %s %s %s %s %s" % (javaCommand, libmcadjar, benchServerClass, node["id"], configPath, gathererNode, gathererPort, logdir, benchDuration)
    sshcmdbg(node["host"], serverCommand, env)


# Client config
# Run client
##export MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n"
##    clientCommand = "mvn "+ mvn_options + " exec:java -Dexec.mainClass=%s -Dexec.args=\"%s %s %s %s %s %s %s\" " % ( benchClientClass, clientId, configPath, messageSize, numPermits, gathererNode, gathererPort, benchDuration)
benchClientClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchClient"
for node in settings.createIdPerNodeList(clientsNodes):
    env = "export APP_HOST=\"%s\" APP_PORT=%s SERVER_HOST=\"%s\" SERVER_PORT=%s ;" % (node["host"], 0, node["host"], 2550)
    clientCommand = "%s -cp %s %s %s %s %s %s %s %s %s" % (javaCommand, libmcadjar,  benchClientClass, node["id"], configPath, messageSize, numPermits, gathererNode, gathererPort, benchDuration)
    sshcmdbg(node["host"], clientCommand, env)

    
# DataGatherer:
#             0         1           2          3         4
# <command> <port> <directory> {<resource> <nodetype> <count>}+

## numClients * numPermits as "load"/as "numClients"?
javagatherercmd = "%s -cp %s %s %s %s" % (javaCommand, libmcadjar, javaGathererClass, gathererPort, logdir)
javagatherercmd += " throughput conservative " + str(numClients)
javagatherercmd += " throughput optimistic   " + str(numClients)
javagatherercmd += " latency    conservative " + str(numClients)
javagatherercmd += " latency    optimistic   " + str(numClients)
javagatherercmd += " latencydistribution conservative " + str(numClients)
javagatherercmd += " latencydistribution optimistic   " + str(numClients)
javagatherercmd += " mistakes   server       " + str(numLearners)

timetowait = benchDuration + (numClients + numGroups * numPxPerGroup * 2 + numLearners) * 10

exitcode = sshcmd(gathererNode, javagatherercmd, timetowait)
if exitcode != 0 :
    localcmd("touch %s/FAILED.txt" % (logdir))

sleep(10)
sys.exit(exitcode)
