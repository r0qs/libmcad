#!/usr/bin/python

# This script initiates the helper nodes, that is, the Ridge Infrastructure:
# All ensembles, each with its coordinator and its acceptors

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

#====================================
#====================================

if len(sys.argv) < 2 :
    system_config_file = script_dir() + "/ridge_2g3e.json"
else :
    system_config_file = os.path.abspath(sys.argv[1])

print("Killing processes listed in " + system_config_file)

config_json = open(system_config_file)

config = json.load(config_json)

# kill all the java processes running in any of the hosts of the ensemble nodes
for process in config["ensemble_processes"] :
    host = process["host"]
    os.system("ssh " + host + " \"killall -9 java\"")
    
print("Done.")