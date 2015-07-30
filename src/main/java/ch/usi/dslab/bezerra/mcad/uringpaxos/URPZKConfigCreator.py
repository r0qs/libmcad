#!/usr/bin/python

'''

 Libmcad - A multicast adaptor library
 Copyright (C) 2015, University of Lugano
 
 This file is part of Libmcad.
 
 Libmcad is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libmcad is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.

'''

__author__ = "Eduardo Bezerra"
__email__  = "eduardo.bezerra@usi.ch"

import sys
import os
import shlex
import subprocess
import json
import time
from kazoo.client import KazooClient

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

def set_parameter(zkclient, path, value="") :
    sval = str(value)
    print "Setting znode " + path + " with value \"" + sval + "\""
    zkclient.create(path, sval, makepath=True)
    
def set_all_parameters(globals, locals, config, zkclient) :
    zk.delete("/ringpaxos", recursive=True)
    for gpar in globals :
        val = globals[gpar] if gpar not in config else config[gpar]
        set_parameter(zkclient, "/ringpaxos/config/" + gpar, val)
    
    for ringcfg in config["rings"] :
        ringid = ringcfg["ring_id"]
        set_parameter(zkclient, "/ringpaxos/rings/" + str(ringid));
        set_parameter(zkclient, "/ringpaxos/topology" + str(ringid) + "/learners");
        for lpar in locals :
            val = locals[lpar] if lpar not in ringcfg else ringcfg[lpar]
            set_parameter(zkclient, "/ringpaxos/topology" + str(ringid) + "/config/" + lpar, val)

# arg1: zknode
# arg2: zkport
# arg3: zkpath

if len(sys.argv) != 5 :
    print "   usage: " + sys.argv[0] + " zknode zkport zkpath configFile"
    sys.exit()

zknode = sys.argv[1]
zkport = sys.argv[2]
zkpath = sys.argv[3]
mcadurpconfigfile = sys.argv[4]


sshcmd(zknode, zkpath + "/bin/zkServer.sh stop")
sshcmd(zknode, zkpath + "/bin/zkServer.sh start")


# config paramaters

local_parameters = {
    "p1_resend_time": 1000,
    "p1_preexecution_number": 5000,
    "concurrent_values": 20,
    "quorum_size": 2,
    "buffer_size": 2097152,
    "trim_quorum": 2,
    "stable_storage": "ch.usi.da.paxos.storage.BufferArray",
    "tcp_nodelay": 1,
    "tcp_crc": 0,
    "learner_recovery": 1,
    "trim_modulo": 0,
    "auto_trim": 0,
    "value_size": 32768,
    "value_count": 900000,
    "value_resend_time": 10000,
    "batch_policy": "none",
    "value_batch_size": 0,
}

global_parameters = {
    "deliver_skip_messages": 1,
    "multi_ring_start_time": current_time_millis(),
    "multi_ring_lambda": 20000,
    "multi_ring_delta_t": 10,
    "multi_ring_m": 1,
    "reference_ring": 0,
    "value_batch_size": 0,
}




mcadurpconfig = open(mcadurpconfigfile)
config = json.load(mcadurpconfig)

num_rings = len(config["rings"])

if "multi_ring_m" in config :
    global_parameters["multi_ring_m"] = config["multi_ring_m"]

zk = KazooClient(hosts=zknode + ":" + zkport)
zk.start()

set_all_parameters(global_parameters, local_parameters, config, zk)