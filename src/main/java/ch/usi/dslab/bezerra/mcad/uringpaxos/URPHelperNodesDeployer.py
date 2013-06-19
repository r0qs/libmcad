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
zookeeper_server_address  = zookeeper_server_location + ":" + config["zookeeper"]["port"]

print("zkLocation: " + zookeeper_server_location + "; zkAddress: " + zookeeper_server_address)




json_data.close()