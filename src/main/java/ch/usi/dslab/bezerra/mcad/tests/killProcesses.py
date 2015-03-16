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
for process in config["ensemble_processes"] + config["group_members"] :
    host = process["host"]
    os.system("ssh " + host + " \"killall -9 java\"")

print("Done.")