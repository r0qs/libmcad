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

javaCommand = "java -XX:+UseG1GC -Xms3g -Xmx3g"
#mvn_options = "-DproxySet=true -DproxyHost=proxy.ufu.br -DproxyPort=3128"
mvn_options = ""

# Directories
SRC = join(expanduser('~'), "src/mestrado/scala")
CFABCASTDIR = join(SRC, "cfabcast")
LIBMCADDEPDIR = join(SRC, "libmcad_dep")
CFABCASTLIBMCADDIR = join(LIBMCADDEPDIR, "libmcad/src/main/java/ch/usi/dslab/bezerra/mcad/cfabcast")
logdir = SRC + "/test/logsmcast"

# Jars
locallibmcadjar = join(SRC, "libmcad/target/libmcad-git.jar")
sensejar = join(LIBMCADDEPDIR, "sense/target/libsense-git.jar")
netwrapperjar = join(LIBMCADDEPDIR, "netwrapper/target/libnetwrapper-git.jar")
ridgejar = join(LIBMCADDEPDIR, "ridge/target/ridge-git.jar")
cfabcastjar = CFABCASTDIR + "/target/scala-2.11/CFABCast-assembly-0.1-SNAPSHOT.jar"
libmcadjar = LIBMCADDEPDIR + "/libmcad/target/libmcad-git.jar"

# cleanup logs
#clean_log(logdir)

# cluster service nodes:
service_nodes = ["node1", "node2", "node3"]

# MONITORING
gathererNode = "localhost"
javaGathererClass = "ch.usi.dslab.bezerra.sense.DataGatherer"
javaDynamicGathererClass = "ch.usi.dslab.bezerra.mcad.benchmarks.DynamicBenchGatherer"
gathererPort = "60000"

# Global config
configPath = CFABCASTLIBMCADDIR + "/config_parameters.json"
numPermits = 1
messageSize = 200
benchDuration = 10
numClients = 1 
numGroups = 1
numPxPerGroup = 1
numLearners = 3
