#!/usr/bin/python

import sys
from benchCommon import farg, sarg, iarg

for arg in sys.argv:
    print arg

#    localcmd(clockSynchronizer)
#     for node in availableNodes :
#         sshcmdbg(node, benchCommon.continousClockSynchronizer)
# 
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