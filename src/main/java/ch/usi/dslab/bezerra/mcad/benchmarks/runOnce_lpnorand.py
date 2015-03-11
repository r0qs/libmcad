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
    cf.write("learner-catch-up yes\n")
    cf.write("tcp-nodelay yes\n")

    storage = "bdb"
    #storage = "lmdb"

    if usedisk :
        cf.write("acceptor-trash-files yes\n")
        if storage == "bdb" :
            cf.write("storage-backend bdb\n")
            cf.write("bdb-sync no\n")
            cf.write("bdb-env-path /tmp/acceptor\n")
            cf.write("bdb-db-filename acc.bdb\n")
        elif storage == "lmdb" :
            cf.write("storage-backend lmdb\n")
            cf.write("lmdb-sync yes\n")
            cf.write("lmdb-env-path /tmp/acceptor\n")
            cf.write("lmdb-mapsize 1gb")
    else :
        cf.write("storage-backend memory\n")
    
    cf.close()

def clean_libpaxos_files(logdir, accnodes) :
    assert logdir.strip() != " "
    localcmd("rm -rf " + logdir + "/*")
    for node in accnodes :
        sshcmd(node, "rm -rf /tmp/acceptor")
################################################################################


################################################################################
# experiment variables
numOutstandings = sarg(1)
numLearners     = sarg(2)
numGroups       = sarg(3)
numPxPerGroup   = sarg(4)
messageSize     = sarg(5)
writeToDisk     = barg(6)
################################################################################

# creating nodepool
nodespool = NodePool()

# numbers of processes
NUM_ACCEPTORS = 3
NUM_PROPOSERS = 1
NUM_LEARNERS = int(numLearners)
NUM_CLIENTS = 1
NUM_OUTSTADINGS = int(numOutstandings)
nodespool.checkSize(NUM_ACCEPTORS + NUM_PROPOSERS + NUM_LEARNERS + NUM_CLIENTS)
acceptors = nodespool.nextn(NUM_ACCEPTORS)
proposers = nodespool.nextn(NUM_PROPOSERS)
learners  = nodespool.nextn(NUM_LEARNERS)
clients   = nodespool.nextn(NUM_CLIENTS)

# create log directory
logdir = get_logdir("lpnorand", 1, NUM_OUTSTADINGS, numLearners, numGroups, numPxPerGroup, messageSize, writeToDisk)
print logdir

# create paxos.conf file
create_libpaxos_configfile(lpnrexecdir + "/paxos.conf", acceptors, proposers, writeToDisk)

''' cleanup : kill processes, erase acceptors' database and erase experiment's logdir
''' 
localcmd(cleaner)
clean_libpaxos_files(logdir, acceptors)

sleep(5)
# start acceptors
for accid in range(NUM_ACCEPTORS) :
    # launch acceptor process accid
    sshcmdbg(acceptors[accid], "%s %s  %s/paxos.conf  > %s/acceptor_%s.log 2>&1" % (lpnracceptor,accid,lpnrexecdir,logdir,accid))
    # launch bw monitor at its node
    sshcmdbg(acceptors[accid], "bwm-ng %s/bwm-ng.conf > %s/acceptor_%s.csv 2>&1" % (                 lpnrexecdir,logdir,accid))

sleep(5)
# start proposers
for propid in range(NUM_PROPOSERS) :
    # launch proposer propid
    sshcmdbg(proposers[propid], "%s %s  %s/paxos.conf  > %s/proposer_%s.log 2>&1" % (lpnrproposer,propid,lpnrexecdir,logdir,propid))
    # bw monitor for proposer propid
    sshcmdbg(proposers[propid], "bwm-ng %s/bwm-ng.conf > %s/proposer_%s.csv 2>&1" % (                  lpnrexecdir,logdir,propid))

sleep(5)
# start learners
for learnerid in range(NUM_LEARNERS) :
    # launch proposer propid
    sshcmdbg(learners[learnerid], "%s     %s/paxos.conf  > %s/learner_%s.log 2>&1" % (lpnrlearner,lpnrexecdir,logdir,learnerid))
    # bw monitor for proposer propid
    sshcmdbg(learners[learnerid], "bwm-ng %s/bwm-ng.conf > %s/learner_%s.csv 2>&1" % (          lpnrexecdir,logdir,learnerid))

sleep(5)
# client
clinode = clients[0]
# start the bw monitor at the client node
sshcmdbg(clinode, "bwm-ng %s/bwm-ng.conf > %s/client_%s.csv 2>&1" % (lpnrexecdir,logdir,1))
# start the client and try to run it 3 times before giving up
exitcode = sshcmd(clinode, "%s %s/paxos.conf %s %s %s %s" % (lpnrclient,lpnrexecdir,0,NUM_OUTSTADINGS,messageSize,1), 60)

# copy client's throughput and latency log
localcmd("mv %s/client1-%s-%sB.csv %s/client_tp_lat.csv" % (HOME,NUM_OUTSTADINGS,messageSize,logdir))

# clean up (for other people)
localcmd(cleaner)

# return experiment (in this case, client's) exit code
sys.exit(exitcode)