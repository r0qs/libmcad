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
numLearners   = iarg(1)
numGroups     = iarg(2)
numPxPerGroup = iarg(3)
messageSize   = iarg(4)
writeToDisk   = barg(5)
initialLoadPerClient =   1
finalLoadPerClient   = 100
################################################################################


################################################################################
logdir = get_logdir_load("ridge", "dynamic", numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk)
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
remainingClients = numGroups
clientNodes = sysConfig.remaining_nodes
while remainingClients > 0 :
    for clinode in clientNodes :
        javaclientcmd = "%s -cp %s %s %s %s %s %s %s %s %s %s" % (javaCommand,       \
             libmcadjar,            dynamicClientClass,   clientId,               \
             ensemblesConfigPath,   messageSize,          sysConfig.gathererNode, \
             gathererPort,          benchDuration,        initialLoadPerClient,   \
             finalLoadPerClient)
        sshcmdbg(clinode, javaclientcmd)
        clientId += 1
        remainingClients -= 1
        if remainingClients <= 0 :
            break

 
# DynamicBenchGatherer:
# args[0] : numDynamicClients
# args[1] : port
# args[2] : directory

# numClients * numPermits as "load"/as "numClients"?
javagatherercmd = "%s -cp %s %s %s %s %s" % (javaCommand, libmcadjar, javaDynamicGathererClass, numGroups, gathererPort, logdir)

timetowait = benchDuration * 10
 
exitcode = sshcmd(sysConfig.gathererNode, javagatherercmd, timetowait)
if exitcode != 0 :
    localcmd("touch %s/FAILED.txt" % (logdir))
     
localcmd(cleaner)
sleep(10)

sys.exit(exitcode)
