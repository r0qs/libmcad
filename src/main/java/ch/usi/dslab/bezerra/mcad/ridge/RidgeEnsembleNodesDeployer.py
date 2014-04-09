#!/usr/bin/python

# This script initiates the helper nodes, that is, the Ridge Infrastructure:
# All ensembles, each with its coordinator and its acceptors

import os
import sys
import json
import threading
from pprint import pprint
from time import sleep

#====================================
#====================================

class launcherThread (threading.Thread):
    def __init__(self, clist):
        threading.Thread.__init__(self)
        self.cmdList = clist
    def run(self):
#         sleep(1)
        for cmd in self.cmdList :
            print ".-rRr-. executing: " + cmd
            os.system(cmd);
#             sleep(0.5)

#====================================
#====================================

system_config_file = sys.argv[1]
xterm = False
if len(sys.argv) > 2 :
    if sys.argv[2] == "x" :
        xterm = True

print("Deploying system helper nodes described in " + system_config_file)

config_json = open(system_config_file)

config = json.load(config_json)

# kill all the java processes running in any of the hosts of the ensemble nodes
for process in config["ensemble_processes"] :
    host = process["host"]
    os.system("ssh " + host + " \"killall -9 java\"")

# MUST ASSUME THAT EACH HELPERNODE IS IN A SINGLE RING
# AND THAT ALL NODES OF THE SAME RING ARE TOGETHER IN THE CONFIG FILE
# AND THAT NO RING HAS ID -1
ensembleCmdLists = []
lastEnsemble = -1
cmdList = []

for process in config["ensemble_processes"] :
    
    role = process["role"]
    pid  = process["pid"]
    ensemble = process["ensemble"]
    host = process["host"]
    port = process["port"]
    
    class_path   = "-cp $HOME/libmcad/target/libmcad-git.jar"    
    node_path    = "ch.usi.dslab.bezerra.mcad.ridge.RidgeEnsembleNode"
    java_string  = "java -XX:+UseG1GC " + class_path + " " + node_path
    arguments    = system_config_file + " " + str(pid)
        
    command_string = ""
    if (xterm == True) :
        command_string = "xterm -geometry 120x20+0+0 -e "
        
    command_string += "ssh " + host + " " + java_string + " " + arguments
    command_string += " &"
    
    cmdList.append(command_string);
    
    print("=== EXECUTING: " + command_string)
    # delete the acceptor's disk backup    
    #os.system(command_string)

config_json.close()

launcherThreads = []

thread = launcherThread(cmdList)
thread.start()
thread.join()
