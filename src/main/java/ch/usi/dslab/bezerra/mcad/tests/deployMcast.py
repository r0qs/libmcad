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

import inspect
import os
import sys
import time

def script_dir():
#    returns the module path without the use of __file__.  Requires a function defined 
#    locally in the module.
#    from http://stackoverflow.com/questions/729583/getting-file-path-of-imported-module
   return os.path.dirname(os.path.abspath(inspect.getsourcefile(lambda _: None)))

# deploying multicast infrastructure
deployer = ridge_deployer = script_dir() + "/../ridge/RidgeEnsembleNodesDeployer.py "
config_file = ridge_config_file = script_dir() + "/ridge_1g1e.json "

urp_deployer = script_dir() + "/../uringpaxos/URPHelperNodesDeployer.py "
# urp_config_file = script_dir() + "/../uringpaxos/configs/urpmcagent_common_1g_1r.json "
urp_config_file = script_dir() + "/urp_1g1r.json "

l = len(sys.argv)
if l not in [2,3] :
    print "usage: " + sys.argv[0] + " urp/ridge [config_file]"
    sys.exit(1)

alg = sys.argv[1]
if alg == "urp" :
    deployer = urp_deployer
    config_file = urp_config_file
elif alg == "ridge" :
    deployer = ridge_deployer
    config_file = ridge_config_file

if l == 3 :
    config_file = sys.argv[2]

# to serialize your own app objects, you must add your own classpath to the mcast deployment
# app_classpath = "/path/to/class/your-app.jar "
app_classpath = ""

# syntax of the deployer:
# ...Deployer.py config.json [additional classpath]
deployment_cmd = deployer + config_file + app_classpath
os.system(deployment_cmd)