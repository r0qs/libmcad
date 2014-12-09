#!/usr/bin/python

import math
import os
import re
import sys
from os.path import expanduser
HOME = expanduser("~")




# ===================================================
# ===================================================
# definitions

benchCommonPath = os.path.dirname(os.path.realpath(__file__)) + "/benchCommon.py"

# available machines
def noderange(first,last) :
    return ["node" + str(val) for val in range(first, last + 1)]

server1="node7"
server2="node8"
fixedNodes = noderange(1,8)
#availableNodes = noderange(41,42) + noderange(44,70)
#availableNodes = noderange(1,10) + noderange(21,40)
availableNodes = noderange(1,40)

# single experiment
onceRunner = HOME + "/libmcad/benchLink/runAllOnce.py"
cleaner = HOME + "/libmcad/benchLink/cleanUp.py"
clockSynchronizer = HOME + "/libmcad/benchLink/clockSynchronizer.py"
continousClockSynchronizer = HOME + "/libmcad/benchLink/continuousClockSynchronizer.py"
systemParamSetter = HOME + "/libmcad/benchLink/systemParamSetter.py"
clockSyncInterval = 3
sysConfigFile = HOME + "libmcad/benchLink/ridge_config.json"

# parameters
javabin = "java -XX:+UseG1GC -Xmx8g"
javacp = "-cp " + HOME + "/libmcad/target/libmcad-git.jar"
duration = "60"

# CLIENTS
numPermits = 1
numUsers = 100000
clientDeployer = HOME + "/libmcad/benchLink/deployTestRunners.py"


# MONITORING
gathererDeployer = HOME + "/chirper/src/main/java/ch/usi/dslab/bezerra/chirper/benchmarks/deployGatherer.py"
javaGathererClass = "ch.usi.dslab.bezerra.sense.DataGatherer"
javaBWMonitorClass = "ch.usi.dslab.bezerra.sense.monitors.BWMonitor"
javaCPUMonitorClass = "ch.usi.dslab.bezerra.sense.monitors.CPUMonitor"
clilogdirRidge = "/tmp/client_log_ridge"
nonclilogdirRidge = "/tmp/nonclient_log_ridge"
gathererBaseLogDir = HOME + "/logsRidge/"
gathererPort = "60000"

# ===================================================
# ===================================================




# ===================================================
# ===================================================
# functions

def getNumLoads(min_cli, max_cli, inc_factor, inc_parcel) :
    numloads = 0
    load = min_cli
    while load <= max_cli :
        numloads += 1
        load = int(math.ceil(load * inc_factor) + inc_parcel)
    return numloads

def freePort(node, port) :
    sshcmd(node, "sudo fuser -k " + str(port) + "/tcp")

def getNid(node) :
    return int(re.findall(r'\d+', node)[0])

def sshcmd(node, cmdstring) :
    print "ssh " + node + " \"" + cmdstring + "\""
    os.system("ssh " + node + " \"" + cmdstring + "\"")
    
def sshcmdbg(node, cmdstring) :
    print "ssh " + node + " \"" + cmdstring + "\" &"
    os.system("ssh " + node + " \"" + cmdstring + "\" &")

def localcmd(cmdstring) :
    print "localcmd: " + cmdstring
    os.system(cmdstring)

def localcmdbg(cmdstring) :
    print "localcmdbg: " + cmdstring
    os.system(cmdstring + " &")

def sarg(i):
    return sys.argv[i]

def iarg(i):
    return int(sarg(i))

def farg(i):
    return float(sarg(i))

def get_index(lst, key, value):
    for i, dic in enumerate(lst):
        if dic[key] == value:
            return i
    return -1

def get_item(lst, key, value):
    index = get_index(lst, key, value)
    if index == -1 : return None
    else           : return lst[index]

# constants
NODE = 0
CLIENTS = 1

def getScreenNode() :
    return availableNodes[0]

def getNonScreenNodes() :
    return availableNodes[1:]

# clientMap is a list of dicts
# clientMap = [{NODE: x, CLIENTS: y}, {NODE: z, CLIENTS: w}]
def mapClientsToNodes(numClients, nodesList) :
    clientMap = []
    clientsPerNode = int(numClients/len(nodesList))
    for node in nodesList :
        clientMap.append({NODE: node, CLIENTS: clientsPerNode})
    for extra in range(numClients % len(nodesList)) :
        clientMap[extra][CLIENTS] += 1
    return clientMap

# clientMap is a list of dicts
# clientMap = [{NODE: x, CLIENTS: y}, {NODE: z, CLIENTS: w}]
def clientNodeIsEmpty(node, clientMap) :
    for mapping in clientMap :
        if mapping[NODE] == node and mapping[CLIENTS] > 0 :
            return False
    return True

def numUsedClientNodes(arg1, arg2 = None) :
    if arg2 == None :
        return numUsedClientNodes_1(arg1)
    elif arg2 != None :
        return numUsedClientNodes_2(arg1, arg2)

def numUsedClientNodes_2(numClients, clientNodes) :
    return min(numClients, len(clientNodes))

def numUsedClientNodes_1(clientNodesMap) :
    numUsed = 0
    for mapping in clientNodesMap :
        if mapping[CLIENTS] > 0 :
            numUsed += 1
    return numUsed

# ===================================================
# ===================================================
