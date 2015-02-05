#!/usr/bin/python

# This script initiates the helper nodes, that is, the URingPaxos Infrastructure:
# All rings, each with its proposer (coordinator) and its acceptors
# (probably they should be proposers themselves too, in case the leader crashes)
#
# This script already cleans up the zookeeper persistent/ephemeral nodes
# and the acceptors' log from BDB

import os
import sys
import json
import inspect
import threading
from pprint import pprint
from time import sleep

#====================================
#====================================

def script_dir():
#    returns the module path without the use of __file__.  Requires a function defined 
#    locally in the module.
#    from http://stackoverflow.com/questions/729583/getting-file-path-of-imported-module
   return os.path.dirname(os.path.abspath(inspect.getsourcefile(lambda _: None)))

class launcherThread (threading.Thread):
    def __init__(self, clist):
        threading.Thread.__init__(self)
        self.cmdList = clist
    def run(self):
#         sleep(1)
        for cmd in self.cmdList :
            print "xXx executing: " + cmd
            os.system(cmd);
            sleep(0.5)

#====================================
#====================================

# sys.argv[1] = config_file

if len(sys.argv) not in [2,3] :
    print "   usage: " + sys.argv[0] + "config_file [x]"
    sys.exit()

system_config_file = sys.argv[1]
xterm = False
if len(sys.argv) > 2 :
    if sys.argv[2] == "x" :
        xterm = True

print("Deploying system helper nodes described in " + system_config_file)

config_json = open(system_config_file)

config = json.load(config_json)



# (re)start zookeeper
zookeeper_server_location = config["zookeeper"]["location"]
zookeeper_server_port = str(config["zookeeper"]["port"])
zookeeper_path = config["zookeeper"]["path"]
zookeeper_server_address  = zookeeper_server_location + ":" + zookeeper_server_port
zookeeper_client_path = os.path.dirname(zookeeper_path) + "/zkCli.sh"

# kill the ring(s) (non-learner nodes)
for node in config["ring_nodes"] :
    node_location = node["node_location"]
    os.system("ssh " + node_location + " \"killall -9 java ; rm -rf /tmp/ringpaxos-db\"")

# URPMCad uses a single zookeeper server, for rendezvous purposes only
# start zkserver standalone for urpaxos
print("[re]starting zookeeper server at " + zookeeper_server_address)
os.system(script_dir() + "/URPZKConfigCreator.py " + zookeeper_server_location + " " + zookeeper_server_port + " " + zookeeper_path + " " + system_config_file)
# os.system("ssh " + zookeeper_server_location + " " + zookeeper_path + " stop")
# os.system("ssh " + zookeeper_server_location + " rm -rf /tmp/zookeeper")
# os.system("ssh " + zookeeper_server_location + " " + zookeeper_path + " start")
# os.system("ssh " + zookeeper_server_location + " " + zookeeper_client_path + " rmr /ringpaxos")

# MUST ASSUME THAT EACH HELPERNODE IS IN A SINGLE RING
# AND THAT ALL NODES OF THE SAME RING ARE TOGETHER IN THE CONFIG FILE
# AND THAT NO RING HAS ID -1
ringCmdLists = []
lastRing = -1
cmdList = []

for node in config["ring_nodes"] :
    nodestring = ""
    for ring in node["node_rings"] :
        if lastRing != ring["ring_id"] :
            cmdList = []
            ringCmdLists.append(cmdList)
            lastRing = ring["ring_id"]
        if len(nodestring) > 0 : nodestring += ";"
        nodestring += str(ring["ring_id"]) + "," + str(node["node_id"]) + ":"
        if "acceptor" in ring["roles"] : nodestring += "A"        
        if "learner"  in ring["roles"] : nodestring += "L"
        if "proposer" in ring["roles"] : nodestring += "P"
    
    node_location = node["node_location"]

    class_path    = "-cp $HOME/libmcad/target/libmcad-git.jar"    
    
    node_path    = "ch.usi.dslab.bezerra.mcad.uringpaxos.URPHelperNode"
    
    java_string  = "java -XX:+UseG1GC " + class_path + " " + node_path
        
    command_string = ""
    if (xterm == True) :
        command_string = "xterm -geometry 120x20+0+0 -e "
        
    command_string += "ssh " + node_location + " " + java_string + " " + zookeeper_server_address  + " " + nodestring

    if "proposer" in ring["roles"] :
        enable_batching="true"
        batch_size=30000
        batch_time=5
        if ring.get("enable_batching") != None and ring["enable_batching"] == False :
            enable_batching="false"
        if ring.get("batch_size_bytes") != None :
            batch_size = ring["batch_size_bytes"]
        if ring.get("batch_time_ms") != None :
            batch_time = ring["batch_time_ms"]

        command_string += " " + str(ring["proposer_port"]) + " " + enable_batching + " " + str(batch_size) + " " + str(batch_time)

    command_string += " &"
    
    cmdList.append(command_string);
    
    #print("=== EXECUTING: " + command_string)
    # delete the acceptor's disk backup    
    #os.system(command_string)

config_json.close()

launcherThreads = []

for clist in ringCmdLists :
    thread = launcherThread(clist)
    thread.start()
    launcherThreads.append(thread);

for t in launcherThreads :
    t.join()
