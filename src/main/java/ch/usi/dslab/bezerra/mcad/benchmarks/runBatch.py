#!/usr/bin/python

from benchCommon import localcmd, availableNodes, clockSynchronizer, systemParamSetter,\
    sshcmdbg, sshcmd, cleaner
import sys
import time
import benchCommon
from math import ceil

from os.path import expanduser
HOME = expanduser("~")

clientNodes = ["node"+str(nid) for nid in range(9, 40 + 1)]

serverClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchServer "
clientClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchClient "
gathererClass="ch.usi.dslab.bezerra.sense.DataGatherer "

localcmd(cleaner)

time.sleep(5)

# localcmd(systemParamSetter)
# localcmd(clockSynchronizer)

incFactor = 1.2
incParcel = 0

minClients = 1
maxClients = 1

clientId = 9
numClients = minClients
while numClients <= maxClients :
    
    for node in availableNodes :
        sshcmdbg(node, benchCommon.continousClockSynchronizer)
    
    deployer = HOME + "/libmcad/src/main/java/ch/usi/dslab/bezerra/mcad/ridge/RidgeEnsembleNodesDeployer.py"
    config = HOME + "/libmcad/benchLink/ridge_config.json"
    localcmd(deployer + " " + config)
    
    javaservercmd = "java -XX:+UseG1GC -Xmx8g -cp " + HOME + "/libmcad/target/libmcad-git.jar " + serverClass
    sshcmdbg("node7", javaservercmd + "7 " + config)
    sshcmdbg("node8", javaservercmd + "8 " + config)
    
    time.sleep(5)
    
    javaclientcmd = "java -XX:+UseG1GC -Xmx8g -cp " + HOME + "/libmcad/target/libmcad-git.jar " + clientClass

    remainingClients = numClients
    while remainingClients > 0 :
        for clinode in clientNodes :
            benchCommon.sshcmdbg(clinode, javaclientcmd + str(clientId) + " " + config + " 100")
            clientId += 1
            remainingClients -= 1
            if remainingClients <= 0 :
                break

    # DataGatherer:
    #             0         1           2          3         4
    # <command> <port> <directory> {<resource> <nodetype> <count>}+
    
    javagatherercmd  = "java -XX:+UseG1GC -Xmx8g -cp " + HOME + "/libmcad/target/libmcad-git.jar " + gathererClass
    javagatherercmd += " 60000 " + "/home/bezerrac/logsRidge/clients_" + str(numClients)
    javagatherercmd += " latency conservative "    + str(numClients)
    javagatherercmd += " latency optimistic "      + str(numClients)
    javagatherercmd += " throughput conservative " + str(numClients)
    javagatherercmd += " throughput optimistic "   + str(numClients)
    javagatherercmd += " mistakes server "         + str(2)
    
    sshcmd("node41", javagatherercmd)
    
    localcmd(benchCommon.cleaner)

    numClients = int(ceil(numClients * incFactor + incParcel))