#!/usr/bin/python

import sys
import os
import shlex
import subprocess
import json
import time

def sshcmd(node, cmdstring) :
    os.system("ssh " + node + " " + cmdstring)

def sshcmdbg(node, cmdstring) :
    run_args = shlex.split("ssh " + node + " " + cmdstring)
    print("xXx ssh " + node + " " + cmdstring)
    return subprocess.Popen(run_args)

def current_time_millis() :
    return int(round(time.time() * 1000))

# returns the ring's specific value for that parater, if exists
# of False, otherwise---in which case, use the default value
def getringconfig(ringid, parameter) :
    return False

def set_parameter(path, value) :
    sval = str(value)
    print "Setting znode " + path + " with value " + sval

# arg1: zknode
# arg2: zkport
# arg3: zkpath

zknode = sys.argv[1]
zkport = sys.argv[2]
zkpath = sys.argv[3]
mcadurpconfigfile = sys.argv[4]


sshcmd(zknode, zkpath + "/bin/zkServer.sh stop")
sshcmd(zknode, zkpath + "/bin/zkServer.sh start")


# config paramaters

global_parameters = {
    "multi_ring_start_time": current_time_millis(),
    "multi_ring_lambda": 20000,
    "multi_ring_delta_t": 10,
    "multi_ring_m": 1,
    "reference_ring": 0
}

local_parameters = {
    "stable_storage": "ch.usi.da.paxos.storage.BufferArray",
    "tcp_nodelay": 1,
    "learner_recovery": 1,
    "trim_modulo": 0,
    "auto_trim": 0,
    #"proposer_batch_policy": "none",    
    "p1_resend_time": 10000,
    "value_resend_time": 10000
}


mcadurpconfig = open(mcadurpconfigfile)
config = json.load(mcadurpconfig)

num_rings = len(config["rings"])

def set_all_parameters(globals, locals, config) :
    for gpar in globals :
        val = globals[gpar] if gpar not in config else config[gpar]
        set_parameter("/ringpaxos/config/" + gpar, val)
    
    for ringcfg in config["rings"] :
        ringid = ringcfg["ring_id"]
        for lpar in locals :
            val = locals[lpar] if lpar not in ringcfg else ringcfg[lpar]
            set_parameter("/ringpaxos/topology" + str(ringid) + "/config/" + lpar, val)

set_all_parameters(global_parameters, local_parameters, config)