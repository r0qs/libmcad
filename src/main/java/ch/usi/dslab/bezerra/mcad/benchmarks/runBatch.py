#!/usr/bin/python

################################################################################
''' imports '''

from benchCommon import *
import sys
import time
import benchCommon
from math import ceil
from os.path import expanduser
################################################################################
''' constants '''

HOME = expanduser("~")
clientNodes = ["node"+str(nid) for nid in range(9, 40 + 1)]
serverClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchServer "
clientClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchClient "
gathererClass="ch.usi.dslab.bezerra.sense.DataGatherer "
################################################################################
''' experiment variables
    - (int)  numClients
    - (int)  numLearners
    - (str)  algorithm
    - (int)  msgSize
    - (dic)  configuration
    - (bool) writeToDisk
    
    order: numClients, config, algorithm, msgSize, writeToDisk
    
'''
##########################################
minClients = 2
maxClients = 50
incFactor = 1.2
incParcel = 0
#numPermits = 1
##########################################
numsLearners = [1, 2, 4, 8, 16, 32]
##########################################
algorithms = ["libpaxos"]#, "mrp", "ridge"]
##########################################
messageSizes = [140, 8192]
##########################################
groups = 0
pxpergroup = 1
groupConfigs = [{groups : 1, pxpergroup : 1},
               # {groups : 1, pxpergroup : 2},
               # {groups : 1, pxpergroup : 4},
               # {groups : 1, pxpergroup : 8},
               # {groups : 2, pxpergroup : 1},
               # {groups : 4, pxpergroup : 1},
               # {groups : 8, pxpergroup : 1},
               ]
##########################################
diskConfigs = [False, True]

################################################################################
''' clean up environment before running batch
'''
# localcmd(cleaner)
# time.sleep(5)
# localcmd(systemParamSetter)
# localcmd(clockSynchronizer)
################################################################################

skips = 0
if len(sys.argv) > 1 :
    skips = iarg(1)

for writeToDisk in diskConfigs :
    for messageSize in messageSizes :
        for algorithm in algorithms :
            for groupConfig in groupConfigs :
                for numLearners in numsLearners :
                    numClients = minClients
                    while numClients <= maxClients :
                        if skips > 0 :
                            print("SKIPPING run %s with %s clients, with %s multicast groups (%s Paxos groups each), message size %s bytes, useDisk is %s" % \
                                  (algorithm, numClients, groupConfig[groups], groupConfig[pxpergroup], messageSize, writeToDisk))
                            skips -= 1
                        else:
                            print("Running %s with %s clients, with %s multicast groups (%s Paxos groups each), message size %s bytes, useDisk is %s" % \
                                  (algorithm, numClients, groupConfig[groups], groupConfig[pxpergroup], messageSize, writeToDisk))
                            localcmd(onceRunner[algorithm] + " %s %s %s %s %s %s" % (numClients, numLearners, groupConfig[groups], groupConfig[pxpergroup], messageSize, writeToDisk))
                        numClients = int(ceil(numClients * incFactor + incParcel))
