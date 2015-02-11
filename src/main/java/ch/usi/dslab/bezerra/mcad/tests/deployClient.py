#!/usr/bin/python

import inspect
import os
import sys
import time
import random

def script_dir():
#    returns the module path without the use of __file__.  Requires a function defined 
#    locally in the module.
#    from http://stackoverflow.com/questions/729583/getting-file-path-of-imported-module
   return os.path.dirname(os.path.abspath(inspect.getsourcefile(lambda _: None)))

random.seed()
rand_id = random.randint(0,2147483647)
sender_id = str(rand_id)

java_bin = "java -XX:+UseG1GC"
debug_port = str(random.randint(20000,65535))
debug_server_str = "-agentlib:jdwp=transport=dt_socket,address=127.0.0.1:" + debug_port + ",server=y,suspend=n"
libmcast_cp = "-cp " + script_dir() + "/../../../../../../../../../target/libmcad-git.jar"

# to serialize your own app objects, you must add your own classpath to the mcast deployment
# app_classpath = "/path/to/class/your-app.jar "
app_classpath = ""

sender_class = "ch.usi.dslab.bezerra.mcad.tests.TestClient"
config_file = script_dir() + "/ridge_1g1e.json"
contact_servers = []# [9, 10, 11, 12]

if len(sys.argv) != 2 :
    print " usage: " + sys.argv[0] + " urp/ridge"
    sys.exit()

if len(sys.argv) == 2 :
    alg = sys.argv[1]
    if alg == "urp" :
        config_file = script_dir() + "/../uringpaxos/configs/urpmcagent_common_1g_1r.json "
#         contact_servers = [14, 15]
    elif alg == "ridge" :
        config_file = script_dir() + "/ridge_1g1e.json"
#         contact_servers = [9, 10, 11, 12]


contact_servers_str = str(contact_servers).replace(',','').strip('[]')

sender_cmd = java_bin + " " + debug_server_str + " " + libmcast_cp + " " + app_classpath + " " + sender_class + " " + config_file + " " + sender_id + " " + contact_servers_str
print sender_cmd
os.system(sender_cmd)
