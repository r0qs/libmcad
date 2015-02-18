#!/usr/bin/python

import sys
from time import sleep
from benchCommon import *

for arg in sys.argv:
    print arg

################################################################################
# create config file for libpaxos
def create_libpaxos_configfile(filepath, acceptors, proposers, usedisk) :
    print ("%s %s %s %s" %(filepath,acceptors, proposers,usedisk))
    cf = open(filepath, "w")
    for accid in range(len(acceptors)) :
        cf.write("acceptor %s 192.168.3.%s %s\n" % (accid , getNid(acceptors[accid ]), 8800 + accid ) )
    for propid in range(len(proposers)) :
        cf.write("proposer %s 192.168.3.%s %s\n" % (propid, getNid(proposers[propid]), 5500 + propid) )
    cf.write("learner-catch-up no\n")
    if usedisk :
        cf.write("storage-backend bdb\n")
        cf.write("bdb-sync yes\n")
        cf.write("bdb-env-path /tmp/acceptor\n")
        cf.write("bdb-db-filename acc.bdb\n")
    else :
        cf.write("storage-backend memory\n")

def clean_libpaxos_files(logdir, accnodes) :
    localcmd("rm -rf " + logdir)
    for node in accnodes :
        sshcmd(node, "rm -rf /tmp/acceptor")
################################################################################


################################################################################
# experiment variables
numClients    = sarg(1)
numLearners   = sarg(2)
numGroups     = sarg(3)
numPxPerGroup = sarg(4)
messageSize   = sarg(5)
writeToDisk   = barg(6)
################################################################################

logdir = get_logdir("libpaxos", numClients, numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk)
print logdir

# creating nodepool
nodespool = NodePool()

# numbers of processes
NUM_ACCEPTORS = 3
NUM_PROPOSERS = 1
NUM_LEARNERS = int(numLearners)
NUM_CLIENTS = 1
NUM_OUTSTADINGS = int(numClients)
nodespool.checkSize(NUM_ACCEPTORS + NUM_PROPOSERS + NUM_LEARNERS + NUM_CLIENTS)
acceptors = nodespool.nextn(NUM_ACCEPTORS)
proposers = nodespool.nextn(NUM_PROPOSERS)
learners  = nodespool.nextn(NUM_LEARNERS)
clients   = nodespool.nextn(NUM_CLIENTS)

# create paxos.conf file
create_libpaxos_configfile(lpexecdir + "/paxos.conf", acceptors, proposers, writeToDisk)

''' cleanup : kill processes, erase acceptors' database and erase experiment's logdir
''' 
localcmd(cleaner)
clean_libpaxos_files(logdir, acceptors)
sleep(3)

# start acceptors
for accid in range(NUM_ACCEPTORS) :
    # launch acceptor process accid
    sshcmdbg(acceptors[accid], "%s %s  %s/paxos.conf  > %s/acceptor_%s.log 2>&1" % (lpacceptor,accid,lpexecdir,logdir,accid))
    # launch bw monitor at its node
    sshcmdbg(acceptors[accid], "bwm-ng %s/bwm-ng.conf > %s/acceptor_%s.csv 2>&1" % (                 lpexecdir,logdir,accid))

# start proposers
for propid in range(NUM_PROPOSERS) :
    # launch proposer propid
    sshcmdbg(proposers[propid], "%s %s  %s/paxos.conf  > %s/proposer_%s.log 2>&1" % (lpproposer,propid,lpexecdir,logdir,propid))
    # bw monitor for proposer propid
    sshcmdbg(proposers[propid], "bwm-ng %s/bwm-ng.conf > %s/proposer_%s.csv 2>&1" % (                  lpexecdir,logdir,propid))

# start learners
for learnerid in range(NUM_LEARNERS) :
    # launch proposer propid
    sshcmdbg(learners[learnerid], "%s     %s/paxos.conf  > %s/learner_%s.log 2>&1" % (lplearner,lpexecdir,logdir,learnerid))
    # bw monitor for proposer propid
    sshcmdbg(learners[learnerid], "bwm-ng %s/bwm-ng.conf > %s/learner_%s.csv 2>&1" % (          lpexecdir,logdir,learnerid))

# client
clinode = clients[0]
# start the bw monitor at the client node
sshcmdbg(clinode, "bwm-ng %s/bwm-ng.conf > %s/client_%s.csv 2>&1" % (lpexecdir,logdir,1))
# start the client
sshcmd(clinode, "%s %s/paxos.conf %s %s %s %s" % (lpclient,lpexecdir,0,NUM_OUTSTADINGS,messageSize,1))
