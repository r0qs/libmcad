# This script initiates the helper nodes, that is, the URingPaxos Infrastructure:
# All rings, each with its proposer (coordinator) and its acceptors
# (probably they should be proposers themselves too, in case the leader crashes)

import os
import sys
import json
from pprint import pprint

system_config_file = sys.argv[1]

print("Deploying system helper nodes described in " + system_config_file)

config_json = open(system_config_file)

config = json.load(config_json)



# (re)start zookeeper
zookeeper_server_location = config["zookeeper"]["location"]
zookeeper_server_port = str(config["zookeeper"]["port"])
zookeeper_path = config["zookeeper"]["path"]
zookeeper_server_address  = zookeeper_server_location + ":" + zookeeper_server_port

print("[re]starting zookeeper server at " + zookeeper_server_address)
os.system("ssh " + zookeeper_server_location + " " + zookeeper_path + " start")

for node in config["helper_nodes"] :
    nodestring = ""    
    for ring in node["node_rings"] :
        if len(nodestring) > 0 : nodestring += ";"
        nodestring += str(ring["ring_id"]) + "," + str(node["node_id"]) + ":"
        if "acceptor" in ring["roles"] : nodestring += "A"        
        if "learner"  in ring["roles"] : nodestring += "L"
        if "proposer" in ring["roles"] : nodestring += "P"
    
    node_location = node["node_location"]
    command_string = "ssh " + node_location + " (path to URPHelperNode class) " + zookeeper_server_address  + " " + nodestring
    
    if "proposer" in ring["roles"] : command_string += " " + str(ring["proposer_port"])
    
    print(command_string)
#     os.system(command_string)




config_json.close()