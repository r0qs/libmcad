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

if len(sys.argv) not in [2,3] :
    print " usage: " + sys.argv[0] + " urp/ridge [config_file]"
    sys.exit()

if len(sys.argv) == 2 :
    alg = sys.argv[1]
    if alg == "urp" :
        config_file = script_dir() + "/urp_1g1r.json"
#         contact_servers = [14, 15]
    elif alg == "ridge" :
        config_file = script_dir() + "/ridge_1g1e.json"
#         contact_servers = [9, 10, 11, 12]

if len(sys.argv) == 3 :
    config_file = sys.argv[2]

contact_servers_str = str(contact_servers).replace(',','').strip('[]')

sender_cmd = java_bin + " " + debug_server_str + " " + libmcast_cp + " " + app_classpath + " " + sender_class + " " + config_file + " " + sender_id + " " + contact_servers_str
print sender_cmd
os.system(sender_cmd)
