#!/usr/bin/python
# -*- coding: utf-8 -*-

from benchCommon import *
import sys, os
from os.path import join, expanduser
from time import sleep

for arg in sys.argv:
    print(arg)

runLocal = False

#SRC = os.getcwd()
SRC = HOME + "/cfabcast/benchmark" 
DEPLOY = join(SRC, "deploy")
DEBUG = join(SRC, "debug")
JARS = join(SRC, "jars")

# Jars
sensejar = join(JARS, "libsense-git.jar")
netwrapperjar = join(JARS, "libnetwrapper-git.jar")
ridgejar = join(JARS, "ridge-git.jar")
cfabcastjar = join(JARS, "CFABCast-assembly-0.1-SNAPSHOT.jar")
libmcadjar = join(JARS, "libmcad-git-allinone.jar")

################################################################################
# functions
def clean_log(logdir) :
    assert logdir.strip() != " "
    localcmd("rm -rf " + logdir + "/*")
################################################################################

#python runOnce_cfabcast.py 1 3 1 1 200 False
################################################################################
# experiment variables
# numero de clientes (BenchClient)
numClients    = iarg(1)
numServers    = numClients

# cada learner tem um BenchServer associado, cria-se numLearners learners, e daí eles são divididos entre os numGroups grupos de multicast
numLearners   = iarg(2)
# numero de grupos de multicast
numGroups     = iarg(3)
# Uma maneira de aumentar throughput é ter vários grupos de acceptors independentes gerando mensagens, e os learners fazem merge determinístico das streams de mensagens. Isso divide a carga de ordenação entre conjuntos de processos paxos independentes, mas aumenta o processamento dos learners e, potencialmente, aumenta a latência se os conjuntos de paxos não estiverem sincronizados.
numPxPerGroup = iarg(4)
# tamanho da msg em bytes
messageSize   = iarg(5)
# quantidade de nós do protocolo:
numService = numLearners

writeToDisk   = barg(6)
################################################################################

################################################################################
# número de requisições que o cliente pode fazer por vez antes de receber alguma resposta
numPermits = numClients

libmcad_logdir = get_logdir("cfabcast", numClients, numPermits, numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk)
print(libmcad_logdir)

# cleanup : kill processes, erase acceptors' database and erase experiment's logdir
localcmd(cleaner)
clean_log(libmcad_logdir)

logdir = libmcad_logdir + "/logs"
localcmd("mkdir -p " + logdir)
gcLogFile = logdir + "/gc.log"

# java options
MEM_OPTS="-Xms1538M -Xmx1538M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=" + logdir + " -XX:+UseParallelGC -XX:+UseCompressedOops"
PRINT_GC_OPTS="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -verbose:gc -Xloggc:" + gcLogFile
TUNNING_GC="-XX:+UseG1GC"
#HOST_OPTS="-Dakka.remote.netty.tcp.hostname=$HOST -Dakka.cluster.seed-nodes.1=akka.tcp://CFABCastSystem@$SEED1 -Dakka.cluster.seed-nodes.2=akka.tcp://CFABCastSystem@$SEED2"
#PORT_OPTS="-Dakka.remote.netty.tcp.port=$PORT"
#LOG_OPTS="-DLOG_DIR=" + cfabcast_log
#DEBUG="-Dakka.loglevel=DEBUG -Dakka.log-dead-letters=1000 -Dakka.remote.log-received-messages=on -Dakka.remote.log-sent-messages=on"
#APP_OPTS="-Dakka.cluster.roles.1=cfabcast"
JAVA_OPTS= MEM_OPTS + " " + PRINT_GC_OPTS
javaCommand="java " + JAVA_OPTS

# libmcad config file
configPath = SRC + "/config_parameters.json"

# TODO get from config
lastNode = availableNodes[len(availableNodes)-1:]
remainingNodes = availableNodes[:len(availableNodes)-1]
serviceNodes = remainingNodes[:numService]
#qtdService = len(serviceNodes)
# same nodes
serverNodes = remainingNodes[:numServers]
clientsNodes = remainingNodes[:numClients]
# distinct nodes
#serverNodes = remainingNodes[qtdService:(qtdService + numServers)]
#clientsNodes = remainingNodes[qtdService:(qtdService + numClients)]
monitorNode = lastNode[0]

gathererNode = monitorNode

if runLocal:
    serviceNodes = ["node1", "node2", "node3"]
    serverNodes = clientsNodes = ["127.0.0.1"]
    monitorNode = "127.0.0.1"

# All service nodes are seeds
#FIXME Resolve name in local runs
port = 2551
counter = 1
contactNodes = seedNodes = ""
for node in serviceNodes:
    seedNodes += "-Dakka.cluster.seed-nodes.%d=\"akka.tcp://CFABCastSystem@%s:%d\" " % (counter, node, port)
    contactNodes += "-Dcontact-points.%d=\"akka.tcp://CFABCastSystem@%s:%d\" " % (counter, node, port)
    counter += 1
    port += 1
print("Seed nodes: " + seedNodes)
print("Contact points: " + contactNodes)
    

def localcmd(cmdstring, timeout=None) :
    print("localcmd: " + cmdstring)
    cmd = Command(cmdstring)
    return cmd.run(timeout)

def localcmdbg(cmdstring, env="") :
    print("localcmdbg: " + env + " " + cmdstring + " &")
    if env != "":
        os.system(env + " " + cmdstring + " &")
    else:
        os.system(cmdstring + " &")

def sshcmd(node, cmdstring, timeout=None) :
    finalstring = "ssh " + node + " \"" + cmdstring + "\""
    print(finalstring)
    cmd = Command(finalstring)
    return cmd.run(timeout)
    
def sshcmdbg(node, cmdstring, env="") :
    print("ssh " + node + " \'" + env + " " + cmdstring + "\' &")
    os.system("ssh " + node + " \'" + env + " " + cmdstring + "\' &")


def getIpOf(hostname):
    if hostname != "127.0.0.1":
        return "192.168.3." + hostname.replace("node", "", 1)
    else:
        return "127.0.0.1"

def createIdPerNodeList(nodes, firstId = 0):
    nodeList = []
    for id in range(firstId, len(nodes)) :
        node = {"id": id, "host" : getIpOf(nodes[id])}
        nodeList.append(node)
    return nodeList

print("Starting service nodes...")
cfabcast_config = join(DEPLOY, "cfabcast-deploy.conf")
if runLocal:
    cfabcast_config = join(DEBUG, "cfabcast-debug.conf")

for node in serviceNodes:
    javaservicecmd = "%s -DLOG_DIR=%s -cp %s -Dconfig.file=%s -Dcfabcast.role.cfproposer.min-nr-of-agents=%d -Dcfabcast.role.acceptor.min-nr-of-agents=%d %s Main %s" % (javaCommand, logdir, cfabcastjar, cfabcast_config, numService, numService, seedNodes, node)
    if runLocal:
        localcmdbg(javaservicecmd, "")
    else:
        sshcmdbg(node, javaservicecmd, "")
sleep(10)

# Run server
server_config = join(DEPLOY, "server-deploy.conf")
if runLocal:
    server_config = join(DEBUG, "server-debug.conf")
    
for node in createIdPerNodeList(serverNodes):
    env = "export APP_HOST=\"%s\" APP_PORT=%s ;" % (node["host"], 2550)
    serverCommand = "%s -cp %s -Dconfig.file=%s %s %s %s %s %s %s %s %s" % (javaCommand, libmcadjar, server_config, contactNodes, benchServerClass, node["id"], configPath, gathererNode, gathererPort, libmcad_logdir, benchDuration)
    if runLocal:
        localcmdbg(serverCommand, env)
    else:
        sshcmdbg(node["host"], serverCommand, env)
sleep(5)

# Run client
client_config = join(DEPLOY, "client-deploy.conf")
if runLocal:
    client_config = join(DEBUG, "client-debug.conf")

for node in createIdPerNodeList(clientsNodes):
    env = "export APP_HOST=\"%s\" APP_PORT=%s SERVER_HOST=\"%s\" SERVER_PORT=%s ;" % (node["host"], 0, node["host"], 2550)
    clientCommand = "%s -cp %s -Dconfig.file=%s %s %s %s %s %s %s %s %s %s" % (javaCommand, libmcadjar, client_config, contactNodes, benchClientClass, node["id"], configPath, messageSize, numPermits, gathererNode, gathererPort, benchDuration)
    if runLocal:
        localcmdbg(clientCommand, env)
    else:
        sshcmdbg(node["host"], clientCommand, env)
    
# DataGatherer:
#             0         1           2          3         4
# <command> <port> <directory> {<resource> <nodetype> <count>}+

## numClients * numPermits as "load"/as "numClients"?
javagatherercmd = "%s -cp %s %s %s %s" % (javaCommand, libmcadjar, javaGathererClass, gathererPort, libmcad_logdir)
javagatherercmd += " throughput conservative " + str(numClients)
javagatherercmd += " throughput optimistic   " + str(numClients)
javagatherercmd += " latency    conservative " + str(numClients)
javagatherercmd += " latency    optimistic   " + str(numClients)
javagatherercmd += " latencydistribution conservative " + str(numClients)
javagatherercmd += " latencydistribution optimistic   " + str(numClients)
javagatherercmd += " mistakes   server       " + str(numServers)

timetowait = benchDuration + (numClients + numGroups * numPxPerGroup * 2 + numServers) * 10

exitcode = 0
if runLocal:
    exitcode = localcmd(javagatherercmd, timetowait)
else:
    exitcode = sshcmd(gathererNode, javagatherercmd, timetowait)

if exitcode != 0 :
    localcmd("touch %s/FAILED.txt" % (libmcad_logdir))

localcmd(cleaner)
sleep(10)

sys.exit(exitcode)
