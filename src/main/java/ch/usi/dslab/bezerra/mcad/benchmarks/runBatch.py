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
gathererClass="ch.usi.dslab.bezerra.sense.DataGatherer "
################################################################################
''' experiment variables
    - (int)  numClients
    - (int)  numLearners
    - (str)  algorithm
    - (int)  msgSize
    - (dic)  configuration
    - (bool) writeToDisk
    
    order given to alg scrip: numClients, numLearners, numGroups, numPxPerGroup, msgSize, writeToDisk
    
'''
##########################################
minClients = 10
maxClients = 70
incFactor = 1
incParcel = 10
#numPermits = 1
##########################################
numsLearners = [1, 2, 4, 8, 16, 32]
#numsLearners = [1, 8, 32]
##########################################
#algorithms = ["libpaxos", "mrp", "ridge"]
algorithms = ["ridge"]
##########################################
messageSizes = [140, 8192, 32768]
#messageSizes = [40000]
##########################################
groups = 0
pxpergroup = 1
groupConfigs = [{groups : 1, pxpergroup : 1},
               # {groups : 1, pxpergroup : 2},
               # {groups : 1, pxpergroup : 4},
               # {groups : 1, pxpergroup : 8},
               {groups : 2, pxpergroup : 1},
               {groups : 4, pxpergroup : 1},
               {groups : 8, pxpergroup : 1},
               ]
##########################################
diskConfigs = [False, True]
#diskConfigs = [False]
################################################################################
''' clean up environment before running batch
'''
localcmd(cleaner)
time.sleep(5)
localcmd(systemParamSetter)
localcmd(clockSynchronizer)
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
                        if groupConfig[groups] > numLearners :
                            print "Not running with less learners than groups"
                        elif skips > 0 :
                            print("SKIPPING run %s with %s clients, with %s multicast groups (%s Paxos groups each), message size %s bytes, useDisk is %s" % \
                                  (algorithm, numClients, groupConfig[groups], groupConfig[pxpergroup], messageSize, writeToDisk))
                            skips -= 1
                        else:
                            tries = 3
                            exitcode = -1
                            while tries > 0 and exitcode != 0 :
                                tries -= 1
                                print("Running %s with %s clients, with %s multicast groups (%s Paxos groups each), message size %s bytes, useDisk is %s" % \
                                     (algorithm, numClients, groupConfig[groups], groupConfig[pxpergroup], messageSize, writeToDisk))
                                exitcode = localcmd(onceRunner[algorithm] + " %s %s %s %s %s %s" % (numClients, numLearners, groupConfig[groups], groupConfig[pxpergroup], messageSize, writeToDisk))
                                if exitcode != 0 :
                                    print("Failed last experiment try")
                        numClients = int(ceil(numClients * incFactor + incParcel))
