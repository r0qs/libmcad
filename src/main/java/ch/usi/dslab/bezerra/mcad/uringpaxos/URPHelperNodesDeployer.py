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
        if "proposer" in ring["roles"] : nodestring += "P"
        if "learner"  in ring["roles"] : nodestring += "L"
    
    node_location = node["node_location"]
    command_string = "ssh " + node_location + " (path to URPHelperNode class) " + nodestring + " " + zookeeper_server_address
    print(command_string)
#     os.system(command_string)




config_json.close()