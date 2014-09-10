#!/usr/bin/python

import inspect
import os
import time

def script_dir():
#    returns the module path without the use of __file__.  Requires a function defined 
#    locally in the module.
#    from http://stackoverflow.com/questions/729583/getting-file-path-of-imported-module
   return os.path.dirname(os.path.abspath(inspect.getsourcefile(lambda _: None)))

sender_id = str(12345)

java_bin = "java -XX:+UseG1GC"
libmcast_cp = "-cp " + script_dir() + "/../../../../../../../../../target/libmcad-git.jar"

# to serialize your own app objects, you must add your own classpath to the mcast deployment
# app_classpath = "/path/to/class/your-app.jar "
app_classpath = ""

sender_class = "ch.usi.dslab.bezerra.mcad.examples.Sender"
config_file = script_dir() + "/ridge_2g3e.json"

sender_cmd = java_bin + " " + libmcast_cp + " " + app_classpath + " " + sender_class + " " + config_file + " " + sender_id
print sender_cmd
os.system(sender_cmd)
