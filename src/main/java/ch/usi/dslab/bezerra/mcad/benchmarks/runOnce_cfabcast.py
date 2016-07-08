#!/usr/bin/python
# -*- coding: utf-8 -*-

from benchCommon import *
import sys, os
from os.path import join, expanduser
from time import sleep

for arg in sys.argv:
    print(arg)

#SRC = os.getcwd()
SRC = HOME + "/cfabcast/benchmark" 
DEPLOY = join(SRC, "deploy")
DEBUG = join(SRC, "debug")

################################################################################
# functions
def clean_log(logdir) :
    assert logdir.strip() != " "
    localcmd("rm -rf " + logdir + "/*")

def elocalcmdbg(cmdstring, out, env="") :
    print("localcmdbg: " + env + " " + cmdstring + " >> " + out + " &")
    if env != "":
        os.system(env + " " + cmdstring + " >> " + out + " &")
    else:
        os.system(cmdstring + " > " + out + " &")

def esshcmdbg(node, cmdstring, out, env="") :
    print("ssh " + node + " \'" + env + " " + cmdstring + " >> " + out + "\' &")
    os.system("ssh " + node + " \'" + env + " " + cmdstring + " >> " + out + "\' &")

def getIpOf(hostname):
    if hostname != "127.0.0.1":
        return "192.168.3." + hostname.replace("node", "", 1)
    else:
        return "127.0.0.1"

# nodes need to be a LIST!
def createIdPerNodeList(nodes, firstId = 0):
    nodeList = []
    for id in range(firstId, len(nodes)) :
        node = {"id": id, "host" : getIpOf(nodes[id])}
        nodeList.append(node)
    return nodeList
################################################################################

#python runOnce_cfabcast.py 1 3 1 1 200 False
################################################################################
# experiment variables
# numero de clientes (BenchClient)
numClients = iarg(1)

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
quorumSize = (numService / 2) + 1 
numCFPs = quorumSize

# número de requisições que o cliente pode fazer por vez antes de receber alguma resposta
numPermits = 10

numServers = numLearners

writeToDisk   = barg(6)
################################################################################

################################################################################

libmcad_logdir = get_logdir("cfabcast", numClients, 1, numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk)
print(libmcad_logdir)

# cleanup : kill processes, erase acceptors' database and erase experiment's logdir
localcmd(cleaner)
clean_log(libmcad_logdir)

logdir = libmcad_logdir + "/logs"
localcmd("mkdir -p " + logdir)
gcLogFile = logdir + "/gc.log"

# java options
MEM_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=" + logdir + " -XX:+UseCompressedOops"
PRINT_GC_OPTS="-XX:+PrintGCDetails -XX:+UseParallelGC -XX:+PrintGCTimeStamps -verbose:gc -Xloggc:" + gcLogFile
TUNNING_GC="-XX:+UseG1GC -Xms3g -Xmx3g"
#HOST_OPTS="-Dakka.remote.netty.tcp.hostname=$HOST -Dakka.cluster.seed-nodes.1=akka.tcp://CFABCastSystem@$SEED1 -Dakka.cluster.seed-nodes.2=akka.tcp://CFABCastSystem@$SEED2"
#PORT_OPTS="-Dakka.remote.netty.tcp.port=$PORT"
#LOG_OPTS="-DLOG_DIR=" + cfabcast_log
#DEBUG="-Dakka.loglevel=DEBUG -Dakka.log-dead-letters=1000 -Dakka.remote.log-received-messages=on -Dakka.remote.log-sent-messages=on"
#APP_OPTS="-Dakka.cluster.roles.1=cfabcast"
#JAVA_OPTS= MEM_OPTS + " " + PRINT_GC_OPTS
JAVA_OPTS= TUNNING_GC
javaCommand="java " + JAVA_OPTS

# libmcad config file
configPath = SRC + "/config_parameters.json"

gathererNode = "node57"
gathererPort = "60000"
benchDuration = 60

# Create nodespool, select servers, clients and gatherer nodes
nodespool = NodePool()
nodespool.checkSize(numLearners + 1) # servers + at least one client
remainingNodes = len(nodespool.all()) - numLearners

if gathererNode == None :
    gathererNode = nodespool.next()
    remainingNodes -= 1
    assert remainingNodes > 0

servers = nodespool.nextn(numLearners)
server_list = createIdPerNodeList(servers)
print(servers)
print(server_list)

client_nodes = []
while remainingNodes > 0:
    client_nodes.append(nodespool.next())
    remainingNodes -= 1
print(client_nodes)

# All service nodes are seeds
#FIXME Resolve name in local runs
port = 2551
counter = 1
contactNodes = seedNodes = ""
for node in servers:
    seedNodes += "-Dakka.cluster.seed-nodes.%d=\"akka.tcp://CFABCastSystem@%s:%d\" " % (counter, node, port)
    contactNodes += "-Dcontact-points.%d=\"akka.tcp://CFABCastSystem@%s:%d\" " % (counter, node, port)
    counter += 1
    port += 1

cfpIds = ""
for n in xrange(1, numCFPs + 1):
    cfpIds += "-Dcfabcast.role.cfproposer.ids.%d=p%d" % (n, n)

print("Seed nodes: " + seedNodes)
print("Contact points: " + contactNodes)


print("Starting SERVICE nodes...")
cfabcast_config = join(DEPLOY, "cfabcast-deploy.conf")
nativeFolder = libmcad_logdir + "/native/"

for node in servers:
    node_stdout = logdir + "/cfabcast-" + node + ".out"
    sigarFolder = nativeFolder + node
    #javaservicecmd = "%s -javaagent:%s -DLOG_DIR=%s -cp %s -Dkamon.system-metrics.sigar-native-folder=%s -Dconfig.file=%s -Dcfabcast.role.cfproposer.min-nr-of-agents=%d -Dcfabcast.min-nr-of-nodes=%d -Dcfabcast.quorum-size=%d %s Main %s" % (javaCommand, aspectjweaverjar, logdir, cfabcastjar, sigarFolder, cfabcast_config, numCFPs, numService, quorumSize, seedNodes, node)
    javaservicecmd = "%s -DLOG_DIR=%s -cp %s -Dconfig.file=%s -Dcfabcast.role.cfproposer.min-nr-of-agents=%d %s -Dcfabcast.min-nr-of-nodes=%d -Dcfabcast.quorum-size=%d %s Main %s" % (javaCommand, logdir, cfabcastjar, cfabcast_config, numCFPs, cfpIds, numService, quorumSize, seedNodes, node)
    esshcmdbg(node, javaservicecmd, node_stdout)
    sleep(1)

# Give a time to Phase1
sleep(60)

# Run server
print("Starting SERVER nodes...")
server_config = join(DEPLOY, "server-deploy.conf")
for node in server_list:
    node_stdout = logdir + "/server-" + str(node["id"]) + ".out"
    env = "export APP_HOST=\"%s\" APP_PORT=%s ;" % (node["host"], 2550)
    serverCommand = "%s -cp %s -Dconfig.file=%s %s %s %s %s %s %s %s %s" % (javaCommand, libmcadjar, server_config, contactNodes, benchServerClass, node["id"], configPath, gathererNode, gathererPort, libmcad_logdir, benchDuration)
    esshcmdbg(node["host"], serverCommand, node_stdout, env)
sleep(5)

# Run client
print("Starting CLIENT nodes...")
client_config = join(DEPLOY, "client-deploy.conf")
clients = createIdPerNodeList(client_nodes)
used_nodes = []
remainingClients = numClients
while remainingClients > 0 :
    for node in clients:
        print("Remaining Clients: " + str(remainingClients) + " numPermits: " + str(numPermits))
        used_nodes.append((node["host"], node["id"], numPermits))
        node_stdout = logdir + "/client-" + str(node["id"]) + ".out"
        server_idx = node["id"] % len(servers)
        server_ip = getIpOf(servers[server_idx])
        env = "export APP_HOST=\"%s\" APP_PORT=%s SERVER_HOST=\"%s\" SERVER_PORT=%s ;" % (node["host"], 0, server_ip, 2550)
        clientCommand = "%s -cp %s -Dconfig.file=%s %s %s %s %s %s %s %s %s %s" % (javaCommand, libmcadjar, client_config, contactNodes, benchClientClass, node["id"], configPath, messageSize, numPermits, gathererNode, gathererPort, benchDuration)
        esshcmdbg(node["host"], clientCommand, node_stdout, env)
        remainingClients -= 1
        if remainingClients <= 0 :
            break
sleep(5)

print used_nodes

# DataGatherer:
#             0         1           2          3         4
# <command> <port> <directory> {<resource> <nodetype> <count>}+

## numClients * numPermits as "load"/as "numClients"?
print("Starting GATHERER node...")
javagatherercmd = "%s -cp %s %s %s %s" % (javaCommand, libmcadjar, javaGathererClass, gathererPort, libmcad_logdir)
javagatherercmd += " throughput conservative " + str(numClients)
javagatherercmd += " throughput optimistic   " + str(numClients)
javagatherercmd += " latency    conservative " + str(numClients)
javagatherercmd += " latency    optimistic   " + str(numClients)
javagatherercmd += " latencydistribution conservative " + str(numClients)
javagatherercmd += " latencydistribution optimistic   " + str(numClients)
javagatherercmd += " mistakes   server       " + str(numServers)

timetowait = benchDuration + 30
#(numClients + numGroups * numPxPerGroup * 2 + numServers) * 10

exitcode = sshcmd(gathererNode, javagatherercmd, timetowait)
if exitcode != 0 :
    localcmd("touch %s/FAILED.txt" % (libmcad_logdir))

localcmd(cleaner)

#### Summary
print "\n----------- SUMMARY -------------"

thr_cmd = "$(grep -v '#' " + libmcad_logdir + "/throughput_conservative_aggregate.log | cut -d' ' -f3)"
print "Messages per second:"
os.system("echo \"" + thr_cmd + "\"")

thr_cmd = "echo \" scale=3; " + thr_cmd + " * 8 * " + str(messageSize) + " / 1000000.0 \" | bc"
print "Mbits per second:"
os.system(thr_cmd)

lat_cmd = "$(grep -v '#' " + libmcad_logdir + "/latency_conservative_average.log | cut -d' ' -f3)"
lat_cmd = "echo \" scale=3; " + lat_cmd + "/ 1000.0 \" | bc"
print "Average latency (micros):"
os.system(lat_cmd)

sys.exit(exitcode)
