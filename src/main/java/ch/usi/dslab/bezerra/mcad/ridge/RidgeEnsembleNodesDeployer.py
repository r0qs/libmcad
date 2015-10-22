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

def script_dir():
#    returns the module path without the use of __file__.  Requires a function defined 
#    locally in the module.
#    from http://stackoverflow.com/questions/729583/getting-file-path-of-imported-module
   return os.path.dirname(os.path.abspath(inspect.getsourcefile(lambda _: None)))

#====================================
#====================================

if len(sys.argv) < 2 :
    print("Usage: " + sys.argv[0] + " config_file + [classpath]")
    
system_config_file = os.path.abspath(sys.argv[1])

extra_classpath = ""
xterm = False
for param in sys.argv[2:] :
    if param == "x" :
        xterm = True
    else :
        extra_classpath += ":" + os.path.abspath(param)

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
    
#    class_path   = "-cp " + script_dir() + "/../../../../../../../../../target/libmcad-git.jar" + extra_classpath
    class_path   = "-cp " + script_dir() + "/../../../../../../../../../../../jars/libmcad-git-allinone.jar" + extra_classpath
    node_path    = "ch.usi.dslab.bezerra.mcad.ridge.RidgeEnsembleNode"
    java_string  = "java -XX:+UseG1GC -Xms3g -Xmx3g " + class_path + " " + node_path
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
