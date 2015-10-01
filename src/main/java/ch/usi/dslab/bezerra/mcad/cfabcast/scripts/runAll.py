#!/usr/bin/python

import os, sys, shlex, threading, subprocess
from os.path import join, expanduser
from time import sleep

def clean_log(logdir) :
    assert logdir.strip() != " "
    localcmd("rm -rf " + logdir + "/*")
    # service logs
    localcmd("rm -rf logs/*.log target/snapshots/ target/shared-journal/")

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

def localcmd(cmdstring, timeout=None) :
    print "localcmd: " + cmdstring
    cmd = Command(cmdstring)
    return cmd.run(timeout)

def localcmdbg(cmdstring) :
    print "localcmdbg: " + cmdstring
    os.system(cmdstring + " &")

SRC = join(expanduser('~'), "src/mestrado/scala")
CFABCASTDIR = join(SRC, "cfabcast")
LIBMCADDEPDIR = join(SRC, "libmcad_dep")
CFABCASTLIBMCADDIR = join(LIBMCADDEPDIR, "libmcad/src/main/java/ch/usi/dslab/bezerra/mcad/cfabcast")

logdir = SRC + "/test/logsmcast"

cfabcastjar = CFABCASTDIR + "/target/scala-2.11/CFABCast-assembly-0.1-SNAPSHOT.jar"
libmcadjar = LIBMCADDEPDIR + "/libmcad/target/libmcad-git.jar"

javaCommand = "java -XX:+UseG1GC -Xms3g -Xmx3g"

# cleanup logs
clean_log(logdir)

# Start cluster service:
service_nodes = ["node1", "node2", "node3"]

for node in service_nodes:
    javaservicecmd = "%s -jar %s %s" % (javaCommand, cfabcastjar, node)
    localcmdbg(javaservicecmd)
sleep(10)

# Global config
configPath = CFABCASTLIBMCADDIR + "/config_parameters.json"
numPermits = 1
messageSize = 8192
benchDuration = 10
numClients = 1 
numGroups = 1
numPxPerGroup = 1
numLearners = 3

# MONITORING
gathererNode = "127.0.0.1"
javaGathererClass = "ch.usi.dslab.bezerra.sense.DataGatherer"
javaDynamicGathererClass = "ch.usi.dslab.bezerra.mcad.benchmarks.DynamicBenchGatherer"
gathererPort = "60000"

# Server Config
benchServerClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchServer"
serverId = 1

# Client config
benchClientClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchClient"
clientId = 1


# Run server
serverCommand = "mvn exec:java -Dexec.mainClass=%s -Dexec.args=\"%s %s %s %s %s %s\" " % ( benchServerClass, \
    serverId, configPath, gathererNode, gathererPort, logdir, benchDuration)
localcmdbg(serverCommand)
sleep(10)

# Run client
clientCommand = "mvn exec:java -Dexec.mainClass=%s -Dexec.args=\"%s %s %s %s %s %s %s\" " % ( benchClientClass, \
    clientId, configPath, messageSize, numPermits, gathererNode, gathererPort, benchDuration)
localcmd(clientCommand)

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

exitcode = sshcmd(gathererNode, javagatherercmd, timetowait)
exitcode = localcmd(javagatherercmd, timetowait)
if exitcode != 0 :
    localcmd("touch %s/FAILED.txt" % (logdir))
