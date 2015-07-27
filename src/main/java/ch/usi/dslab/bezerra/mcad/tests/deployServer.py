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

if len(sys.argv) not in [3,4] :
    print "usage: " + sys.argv[0] + " <receiver id> <urp/ridge> [<config_file>]"
    sys.exit(1)
receiver_id = sys.argv[1] + " "
alg = sys.argv[2]
if len(sys.argv) == 4 :
    config_file = sys.argv[3]
else :
    if alg == "urp" :
        config_file = script_dir() + "/urp_1g1r.json "
    elif alg == "ridge" :
        config_file = script_dir() + "/ridge_1g1e.json "


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

silence_receivers = False
#silence_receivers = True

receiver_cmd = java_bin + " " + debug_server_str + " " + jmx_server_str + " " + libmcast_cp + " " + app_classpath + " " + receiver_class + " " + receiver_id + " " + config_file
if silence_receivers : receiver_cmd += " &> /dev/null "
print receiver_cmd
os.system(receiver_cmd)