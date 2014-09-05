#!/usr/bin/python

import os
import time
import sys

sender_id = str(12345)

java_bin = "java -XX:+UseG1GC"
libmcast_cp = "-cp /Users/eduardo/libmcad/target/libmcad-git.jar"

# to serialize your own app objects, you must add your own classpath to the mcast deployment
# app_classpath = "/path/to/class/your-app.jar "
app_classpath = ""

sender_class = "ch.usi.dslab.bezerra.mcad.examples.Sender"
config_file = "/Users/eduardo/repositories/mine/libmcad/src/main/java/ch/usi/dslab/bezerra/mcad/examples/ridge_2g3e.json"

sender_cmd = java_bin + " " + libmcast_cp + " " + app_classpath + " " + sender_class + " " + config_file + " " + sender_id
print sender_cmd
os.system(sender_cmd)
