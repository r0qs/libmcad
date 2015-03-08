#!/usr/bin/python

import math, os, re, sys, shlex, threading, subprocess
from os.path import expanduser
HOME = expanduser("~")




# ===================================================
# ===================================================
# definitions

benchCommonPath = os.path.dirname(os.path.realpath(__file__)) + "/benchCommon.py"

# ============================
# available machines
def noderange(first,last) :
    return ["node" + str(val) for val in range(first, last + 1)]

#availableNodes = noderange(1,34) + noderange(41,50)
availableNodes = noderange(1,35) + noderange(41,52) + noderange(54,60) + noderange(62,64)

def testNodes() :
    for n in availableNodes :
        exitcode = localcmd("ssh %s uname -a" % (n))
        assert exitcode == 0

class NodePool:
    nodePointer = -1
    nodes = availableNodes
    def last(self):
        assert self.nodePointer in range(len(self.nodes))
        return self.nodes[self.nodePointer]
    def next(self) :
        self.nodePointer += 1
        return self.last()
    def checkSize(self, num) :
        assert num <= len(self.nodes)
    def all(self) :
        return list(self.nodes)
    def nextn(self, n):
        ret = []
        for _ in range (n) :
            ret.append(self.next())
        return ret
# ============================

def get_logdir(algorithm, numClients, numPermits, numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk):
    return get_logdir_load(algorithm, int(numClients) * int(numPermits), numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk)

def get_logdir_load(algorithm, load, numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk):
    dirpath = logdir + "/%s/%s_%s_clients_%s_learners_%s_groups_%s_pxpergroup_%s_bytes_diskwrite_%s" % \
                (algorithm, algorithm, load, numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk)
    localcmd("mkdir -p " + dirpath)
    return dirpath

# single experiment
onceRunner = {"libpaxos" : HOME + "/libmcad/benchLink/runOnce_libpaxos.py",
              "mrp"      : HOME + "/libmcad/benchLink/runOnce_mrp.py",
              "ridge"    : HOME + "/libmcad/benchLink/runOnce_ridge.py" }
cleaner = HOME + "/libmcad/benchLink/cleanUp.py"
clockSynchronizer = HOME + "/libmcad/benchLink/clockSynchronizer.py"
continousClockSynchronizer = HOME + "/libmcad/benchLink/continuousClockSynchronizer.py"
clockSyncInterval = 3
systemParamSetter = HOME + "/libmcad/benchLink/systemParamSetter.py"
libmcadjar = HOME + "/libmcad/target/libmcad-git.jar"
javaCommand = "java -XX:+UseG1GC -Xms3g -Xmx3g"
benchServerClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchServer"
benchClientClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchClient"
dynamicClientClass = "ch.usi.dslab.bezerra.mcad.benchmarks.DynamicBenchClient"
benchDuration = 60

# batching parameters
batch_size_threshold_bytes_memory = 0
batch_time_threshold_ms_memory    = 0
batch_size_threshold_bytes_disk = 0
batch_time_threshold_ms_disk    = 0

# libpaxos
lpexecdir  = HOME + "/paxosudp/build/sample"
lpacceptor = lpexecdir + "/acceptor"
lpproposer = lpexecdir + "/proposer"
lplearner  = lpexecdir + "/learner"
lpclient   = lpexecdir + "/client"
logdir   = HOME + "/logsmcast/"

# ridge
delta_null_messages_ms_disk = 30
delta_null_messages_ms_memory = 5
latency_estimation_sample = 10
latency_estimation_devs = 0
latency_estimation_max = 10
ridgeDeployer = HOME + "/libmcad/src/main/java/ch/usi/dslab/bezerra/mcad/ridge/RidgeEnsembleNodesDeployer.py"
ridge_disk_storage_type = "bdbasync"
#ridge_memory_storage_type = "memcache" #"nostorage" #"memory"
#ridge_memory_storage_type = "fastarray"
#ridge_memory_storage_type = "nullstorage"
ridge_memory_storage_type = "listcache"

# CLIENTS
numPermits = 1
numUsers = 100000
clientDeployer = HOME + "/libmcad/benchLink/deployTestRunners.py"


# MONITORING
gathererDeployer = HOME + "/chirper/src/main/java/ch/usi/dslab/bezerra/chirper/benchmarks/deployGatherer.py"
javaGathererClass = "ch.usi.dslab.bezerra.sense.DataGatherer"
javaDynamicGathererClass = "ch.usi.dslab.bezerra.mcad.benchmarks.DynamicBenchGatherer"
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
# useful classes and functions

class Command(object):
    def __init__(self, cmd):
        self.cmd = cmd
        self.process = None

    def run(self, timeout):
        def target():
            print 'Thread started'
            run_args = shlex.split(self.cmd)
            self.process = subprocess.Popen(run_args)
            self.process.communicate()
            print 'Thread finished'

        thread = threading.Thread(target=target)
        thread.start()

        thread.join(timeout)
        if thread.is_alive():
            print 'Terminating process'
            self.process.terminate()
            thread.join()
        return self.process.returncode
    
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

def sshcmd(node, cmdstring, timeout=None) :
    finalstring = "ssh " + node + " \"" + cmdstring + "\""
    print finalstring
    cmd = Command(finalstring)
    return cmd.run(timeout)
    
def sshcmdbg(node, cmdstring) :
    print "ssh " + node + " \"" + cmdstring + "\" &"
    os.system("ssh " + node + " \"" + cmdstring + "\" &")

def localcmd(cmdstring, timeout=None) :
    print "localcmd: " + cmdstring
    cmd = Command(cmdstring)
    return cmd.run(timeout)

def localcmdbg(cmdstring) :
    print "localcmdbg: " + cmdstring
    os.system(cmdstring + " &")

def sarg(i) :
    return sys.argv[i]

def iarg(i) :
    return int(sarg(i))

def farg(i) :
    return float(sarg(i))

def barg(i) :
    return sarg(i) in ["True", "true", "T", "t", 1]

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
