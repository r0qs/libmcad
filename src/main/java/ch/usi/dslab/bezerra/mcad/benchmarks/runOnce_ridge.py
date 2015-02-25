#!/usr/bin/python

import sys
from time import sleep
from benchCommon import *
from systemConfigurer_ridge import *

for arg in sys.argv:
    print arg


################################################################################
# functions
def clean_ridge_log(logdir) :
    assert logdir.strip() != " "
    localcmd("rm -rf " + logdir + "/*")
        
def clean_ridge_storage(accnodes) :
    for node in accnodes :
        sshcmd(node["host"], "rm -rf /tmp/ridge-bdb")
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
logdir = get_logdir("ridge", numClients, numPermits, numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk)
print logdir
clean_ridge_log(logdir)

# creating nodepool
nodespool = NodePool()

# create config files
ensemblesConfigPath  = logdir + "/ensembles_config.json"
partitionsConfigPath = logdir + "/partitions_config.json"
sysConfig = generateRidgeSystemConfiguration(nodespool.all(), numGroups, numPxPerGroup, numLearners, 3, writeToDisk, ensemblesConfigPath, partitionsConfigPath, saveToFile = True)
if sysConfig == None :
    sys.exit(1)

# cleanup : kill processes, erase acceptors' database and erase experiment's logdir
localcmd(cleaner)
clean_ridge_storage(sysConfig.acceptor_list + sysConfig.coordinator_list)

# clock synchronizer (necessary for efficient merging from multiple ensembles)
for node in nodespool.all() :
    sshcmdbg(node, continousClockSynchronizer)

# start ensembles
localcmd(ridgeDeployer + " " + ensemblesConfigPath)

# start servers
for serverProcess in sysConfig.server_list :
    print serverProcess
    # server = {"id": sid, "partition": gid, "host" : nodes[sid], "pid" : sid, "role" : "server"}
    javaservercmd = "%s -cp %s %s %s %s %s %s %s %s" % (javaCommand,      \
         libmcadjar,           benchServerClass,        serverProcess["id"], \
         ensemblesConfigPath,  sysConfig.gathererNode,  gathererPort,        \
         logdir,               benchDuration)
    sshcmdbg(serverProcess["host"], javaservercmd)
sleep(5)

# start clients
clientId = sysConfig.client_initial_pid
remainingClients = numClients
clientNodes = sysConfig.remaining_nodes
while remainingClients > 0 :
    for clinode in clientNodes :
        javaclientcmd = "%s -cp %s %s %s %s %s %s %s %s %s" % (javaCommand,
             libmcadjar,             benchClientClass,   clientId,    \
             ensemblesConfigPath,    messageSize,        numPermits,  \
             sysConfig.gathererNode, gathererPort,       benchDuration)
        sshcmdbg(clinode, javaclientcmd)
        clientId += 1
        remainingClients -= 1
        if remainingClients <= 0 :
            break

 
# DataGatherer:
#             0         1           2          3         4
# <command> <port> <directory> {<resource> <nodetype> <count>}+

# numClients * numPermits as "load"/as "numClients"?
javagatherercmd = "%s -cp %s %s %s %s" % (javaCommand, libmcadjar, javaGathererClass, gathererPort, logdir)
javagatherercmd += " latency    conservative " + str(numClients)
javagatherercmd += " latency    optimistic   " + str(numClients)
javagatherercmd += " throughput conservative " + str(numClients)
javagatherercmd += " throughput optimistic   " + str(numClients)
javagatherercmd += " mistakes   server       " + str(numLearners)
    
exitcode = sshcmd(sysConfig.gathererNode, javagatherercmd, benchDuration + 60)
     
localcmd(benchCommon.cleaner)
sleep(10)

sys.exit(exitcode)
