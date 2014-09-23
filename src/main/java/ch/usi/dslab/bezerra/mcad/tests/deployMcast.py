#!/usr/bin/python

import inspect
import os
import sys
import time

def script_dir():
#    returns the module path without the use of __file__.  Requires a function defined 
#    locally in the module.
#    from http://stackoverflow.com/questions/729583/getting-file-path-of-imported-module
   return os.path.dirname(os.path.abspath(inspect.getsourcefile(lambda _: None)))

# deploying (for now only ridge) multicast infrastructure
ridge_deployer = script_dir() + "/../ridge/RidgeEnsembleNodesDeployer.py "
config_file = script_dir() + "/ridge_2g3e.json "

if len(sys.argv) > 1 :
    config_file = sys.argv[1]

# to serialize your own app objects, you must add your own classpath to the mcast deployment
# app_classpath = "/path/to/class/your-app.jar "
app_classpath = ""

# syntax of the deployer:
# ...Deployer.py config.json [additional classpath]
deployment_cmd = ridge_deployer + config_file + app_classpath
os.system(deployment_cmd)