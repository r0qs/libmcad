#!/usr/bin/python

import sys
from benchCommon import *
from systemConfigurer_ridge import *

for arg in sys.argv:
    print arg

################################################################################
# experiment variables
numClients    = iarg(1)
numLearners   = iarg(2)
numGroups     = iarg(3)
numPxPerGroup = iarg(4)
messageSize   = iarg(5)
writeToDisk   = barg(6)
################################################################################

logdir = get_logdir("ridge", numClients, numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk)
print logdir

# creating nodepool
nodespool = NodePool()

# create config files
ensemblesConfigPath  = logdir + "/ensembles_config.json"
partitionsConfigPath = logdir + "/partitions_config.json"
sysConfig = generateRidgeSystemConfiguration(nodespool.all(), numGroups, numPxPerGroup, numLearners, 3, writeToDisk, ensemblesConfigPath, partitionsConfigPath, saveToFile = True)
if sysConfig == None :
    sys.exit(1)

# clock synchronizer (necessary for efficient merging from multiple ensembles)
for node in nodespool.all() :
    sshcmdbg(node, continousClockSynchronizer)

# start ensembles
#localcmd(ridgeDeployer + " " + ensemblesConfigPath)

# start servers
for serverNode in sysConfig.server_list :
    print serverNode
    # server = {"id": sid, "partition": gid, "host" : nodes[sid], "pid" : sid, "role" : "server"}

#     deployer = HOME + "/libmcad/src/main/java/ch/usi/dslab/bezerra/mcad/ridge/RidgeEnsembleNodesDeployer.py"
#     config = HOME + "/libmcad/benchLink/ridge_config.json"
#     localcmd(deployer + " " + config)
#     
#     javaservercmd = "java -XX:+UseG1GC -Xmx8g -cp " + HOME + "/libmcad/target/libmcad-git.jar " + serverClass
#     sshcmdbg(benchCommon.server1, javaservercmd + "7 " + config)
#     sshcmdbg(benchCommon.server2, javaservercmd + "8 " + config)
#     
#     time.sleep(5)
#     
#     javaclientcmd = "java -XX:+UseG1GC -Xmx8g -cp " + HOME + "/libmcad/target/libmcad-git.jar " + clientClass
# 
#     clientId = 9
#     remainingClients = numClients
#     while remainingClients > 0 :
#         for clinode in clientNodes :
#             benchCommon.sshcmdbg(clinode, javaclientcmd + str(clientId) + " " + config + " 100 " + str(numPermits))
#             clientId += 1
#             remainingClients -= 1
#             if remainingClients <= 0 :
#                 break
# 
#     # DataGatherer:
#     #             0         1           2          3         4
#     # <command> <port> <directory> {<resource> <nodetype> <count>}+
#     
#     javagatherercmd  = "java -XX:+UseG1GC -Xmx8g -cp " + HOME + "/libmcad/target/libmcad-git.jar " + gathererClass
#     javagatherercmd += " 60000 " + "/home/bezerrac/logsRidge/load_" + str(numClients * numPermits)
#     javagatherercmd += " latency conservative "    + str(numClients)
#     javagatherercmd += " latency optimistic "      + str(numClients)
#     javagatherercmd += " throughput conservative " + str(numClients)
#     javagatherercmd += " throughput optimistic "   + str(numClients)
#     javagatherercmd += " mistakes server "         + str(2)
#     
#     sshcmd("node40", javagatherercmd)
#     
#     localcmd(benchCommon.cleaner)
#     time.sleep(10)