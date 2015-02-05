#!/usr/bin/python

import inspect
import os
import time
import sys

def script_dir():
#    returns the module path without the use of __file__.  Requires a function defined 
#    locally in the module.
#    from http://stackoverflow.com/questions/729583/getting-file-path-of-imported-module
   return os.path.dirname(os.path.abspath(inspect.getsourcefile(lambda _: None)))

# one of the receivers should start with parameters:  9 1
#   the other receiver should start with parameters: 10 2
# 
# The first parameter is the node id, and the second one is the group id to which the node belongs.
# Such node must be in the ridge configuration file, under the *group_members* section. This
# means that (for now) the whole system configuration is static, given in the config file.

if len(sys.argv) != 2 and len(sys.argv) != 3 :
    print "usage: " + sys.argv[0] + " <receiver id> [<config_file>]"
    sys.exit(1)
receiver_id = sys.argv[1] + " "

java_bin = "java -XX:+UseG1GC"
libmcast_cp = "-cp " + script_dir() + "/../../../../../../../../../target/libmcad-git.jar"

debug_port = str(40000 + int(receiver_id))
debug_server_str = "-agentlib:jdwp=transport=dt_socket,address=127.0.0.1:" + debug_port + ",server=y,suspend=n"

jmx_port = str(30000 + int(receiver_id))
jmx_server_str = "-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=" + jmx_port + " -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"

# to serialize your own app objects, you must add your own classpath to the mcast deployment
# app_classpath = "/path/to/class/your-app.jar "
app_classpath = ""

receiver_class = "ch.usi.dslab.bezerra.mcad.tests.TestServer"



if len(sys.argv) > 2 :
    alg = sys.argv[2]
    if alg == "urp" :
        config_file = script_dir() + "/../uringpaxos/configs/urpmcagent_server_only.json "
    elif alg == "ridge" :
        config_file = script_dir() + "/ridge_2g3e.json "

silence_receivers = False
#silence_receivers = True

receiver_cmd = java_bin + " " + debug_server_str + " " + jmx_server_str + " " + libmcast_cp + " " + app_classpath + " " + receiver_class + " " + receiver_id + " " + config_file
if silence_receivers : receiver_cmd += " &> /dev/null "
print receiver_cmd
os.system(receiver_cmd)