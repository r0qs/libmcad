#!/usr/bin/python

import sys
from time import sleep
from benchCommon import *
from systemConfigurer_ridge import *

for arg in sys.argv:
    print arg


################################################################################
# functions
def clean_log(logdir) :
    assert logdir.strip() != " "
    localcmd("rm -rf " + logdir + "/*")
 
# TODO remove target directory 
#def clean_ridge_storage(accnodes) :
#    for node in accnodes :
#        sshcmd(node["host"], "rm -rf /tmp/ridge-bdb")
################################################################################


################################################################################
# experiment variables
numClients    = iarg(1)
numLearners   = iarg(2)
numGroups     = iarg(3)
numPxPerGroup = iarg(4)
messageSize   = iarg(5)
writeToDisk   = barg(6)
################################################################################


################################################################################
numPermits = numClients
numClients = numGroups * numPxPerGroup
logdir = get_logdir("cfabcast", numClients, numPermits, numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk)
print logdir
#clean_ridge_log(logdir)

# creating nodepool
nodespool = NodePool()

# cleanup : kill processes, erase acceptors' database and erase experiment's logdir
def cleanProcessOf(user):
    for node in benchCommon.availableNodes :
        # print "Cleaning worker " + node + "..."
        benchCommon.sshcmdbg(node, "killall -9 -u " + user + " &> /dev/null")
        benchCommon.localcmdbg("ssh lasaro@node249 ssh " + node + " sudo killall -9 -u " + user + " &> /dev/null")
    time.sleep(5)

#localcmd(cleanProcessOf("lasaro"))

# TODO
#clean_ridge_storage(sysConfig.acceptor_list + sysConfig.coordinator_list)

# clock synchronizer (necessary for efficient merging from multiple ensembles)
#for node in nodespool.all() :
#    sshcmdbg(node, continousClockSynchronizer)


# (41 to 60)
gathererNode = availableNodes[0] 

# Server nodes
server_list = availableNodes[1]

# Clients nodes
client_nodes = availableNodes[1]

# Service nodes
service_nodes = availableNodes[2:]

from os.path import join, expanduser
SRC = join(expanduser('~'), "src/mestrado/scala")
CFABCASTDIR = join(SRC, "cfabcast")
LIBMCADDEPDIR = join(CFABCASTDIR, "libmcad_dep")

#sbtCommand = HOME + "/sbt/bin/sbt"
sbtCommand = "sbt"

# start CFABCast service on all nodes of cluster
def runSbtOn(node) = sbtCommand + " 'run-main Main %s'" % node
for node in service_nodes :
    localcmdbg(join(CFABCASTDIR, "sbt 'run-main Main %s'" % node))
#    sshcmdbg(node, runSbtOn(node))

ensemblesConfigPath = ""

# start servers
#mvn exec:java -Dexec.mainClass="sample.bench.BenchServer" -Dexec.args="1"
serverId = 1
for serverProcess in server_list :
    print serverProcess
    javaservercmd = "%s -cp %s %s %s %s %s %s %s %s" % (javaCommand,      \
         libmcadjar,           benchServerClass,        serverId, \
         ensemblesConfigPath,  gathererNode,  gathererPort,        \
         logdir,               benchDuration)
    localcmdbg(javaservercmd)
    serverId += 1
#    sshcmdbg(serverProcess["host"], javaservercmd)
sleep(5)

# start clients
#mvn exec:java -Dexec.mainClass="sample.bench.BenchClient" -Dexec.args="1"
clientId = 1 
remainingClients = numClients
clientNodes = client_nodes
while remainingClients > 0 :
    for clinode in clientNodes :
        javaclientcmd = "%s -cp %s %s %s %s %s %s %s %s %s" % (javaCommand,
             libmcadjar,             benchClientClass,   clientId,    \
             ensemblesConfigPath,    messageSize,        numPermits,  \
             gathererNode, gathererPort,       benchDuration)
        localcmdbg(javaclientcmd)
#        sshcmdbg(clinode, javaclientcmd)
        clientId += 1
        remainingClients -= 1
        if remainingClients <= 0 :
            break

 
# DataGatherer:
#             0         1           2          3         4
# <command> <port> <directory> {<resource> <nodetype> <count>}+

# numClients * numPermits as "load"/as "numClients"?
javagatherercmd = "%s -cp %s %s %s %s" % (javaCommand, libmcadjar, javaGathererClass, gathererPort, logdir)
javagatherercmd += " throughput conservative " + str(numClients)
javagatherercmd += " throughput optimistic   " + str(numClients)
javagatherercmd += " latency    conservative " + str(numClients)
javagatherercmd += " latency    optimistic   " + str(numClients)
javagatherercmd += " latencydistribution conservative " + str(numClients)
javagatherercmd += " latencydistribution optimistic   " + str(numClients)
javagatherercmd += " mistakes   server       " + str(numLearners)

timetowait = benchDuration + (numClients + numGroups * numPxPerGroup * 2 + numLearners) * 10

#exitcode = sshcmd(gathererNode, javagatherercmd, timetowait)
exitcode = localcmd(gathererNode, javagatherercmd, timetowait)
if exitcode != 0 :
    localcmd("touch %s/FAILED.txt" % (logdir))
     
#localcmd(cleaner)
sleep(10)

sys.exit(exitcode)
