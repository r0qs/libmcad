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
from pprint import pprint
from time import sleep

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
os.system("ssh " + zookeeper_server_location + " " + zookeeper_path + " start")
os.system("ssh " + zookeeper_server_location + " " + zookeeper_client_path + " rmr /ringpaxos")

slept = False

for node in config["ring_nodes"] :
    sleep(0.2)
#    if not slept :
#        sleep(1)
#        slept = True
    nodestring = ""    
    for ring in node["node_rings"] :
        if len(nodestring) > 0 : nodestring += ";"
        nodestring += str(ring["ring_id"]) + "," + str(node["node_id"]) + ":"
        if "acceptor" in ring["roles"] : nodestring += "A"        
        if "learner"  in ring["roles"] : nodestring += "L"
        if "proposer" in ring["roles"] : nodestring += "P"
    
    node_location = node["node_location"]
    #library_path = "-Djava.library.path=$HOME/uringpaxos/target/build/Paxos-trunk/lib"
    
    class_path   = "-cp $CLASSPATH"
    # new claspath
    class_path  += ":$HOME/libmcad/target/libmcad-1.jar"    
    class_path  += ":$HOME/software/java_libs/*"
    class_path  += ":$HOME/uringpaxos/target/paxos-trunk.jar"
    class_path  += ":$HOME/uringpaxos/target/build/Paxos-trunk/lib/*"
    # old classpath
    #class_path  += ":$HOME/libmcad/bin/"
    #class_path  += ":$HOME/software/java_libs/*"
    #class_path  += ":$HOME/uringpaxos/target/build/Paxos-trunk/lib/*"
    
    node_path    = "ch.usi.dslab.bezerra.mcad.uringpaxos.URPHelperNode"
    
    #java_string  = "java " + library_path + " " + class_path + " " + node_path
    java_string  = "java " + class_path + " " + node_path
        
    command_string = ""
    if (xterm == True) :
        command_string = "xterm -geometry 120x20+0+0 -e "
        
    command_string += "ssh " + node_location + " " + java_string + " " + zookeeper_server_address  + " " + nodestring
    
    if "proposer" in ring["roles"] : command_string += " " + str(ring["proposer_port"])
    
    command_string += " &"
    
    
    print("=== EXECUTING: " + command_string)
    # delete the acceptor's disk backup    
    os.system(command_string)
    
    #os.system("sleep 1")
    #sleep(0.2)

#    if not slept :
#        sleep(1)
#        slept = True




config_json.close()
