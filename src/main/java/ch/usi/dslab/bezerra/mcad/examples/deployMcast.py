#!/usr/bin/python

import os
import time

# deploying (for now only ridge) multicast infrastructure

ridge_deployer = "/Users/eduardo/libmcad/src/main/java/ch/usi/dslab/bezerra/mcad/ridge/RidgeEnsembleNodesDeployer.py "
config_file = "/Users/eduardo/repositories/mine/libmcad/src/main/java/ch/usi/dslab/bezerra/mcad/examples/ridge_2g3e.json "

# to serialize your own app objects, you must add your own classpath to the mcast deployment
# app_classpath = "/path/to/class/your-app.jar "
app_classpath = ""

# syntax of the deployer:
# ...Deployer.py config.json [additional classpath]
deployment_cmd = ridge_deployer + config_file + app_classpath
os.system(deployment_cmd)