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

# deploying multicast infrastructure
deployer = ridge_deployer = script_dir() + "/../ridge/RidgeEnsembleNodesDeployer.py "
config_file = ridge_config_file = script_dir() + "/ridge_2g3e.json "

urp_deployer = script_dir() + "/../uringpaxos/URPHelperNodesDeployer.py "
urp_config_file = script_dir() + "/../uringpaxos/configs/urpmcagent_common_1g_1r.json "

l = len(sys.argv)
if l > 1 :
    alg = sys.argv[1]
    if alg == "urp" :
        deployer = urp_deployer
        config_file = urp_config_file
    elif alg == "ridge" :
        deployer = ridge_deployer
        config_file = ridge_config_file

# to serialize your own app objects, you must add your own classpath to the mcast deployment
# app_classpath = "/path/to/class/your-app.jar "
app_classpath = ""

# syntax of the deployer:
# ...Deployer.py config.json [additional classpath]
deployment_cmd = deployer + config_file + app_classpath
os.system(deployment_cmd)