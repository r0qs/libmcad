#!/usr/bin/python

from time import sleep
from settings import *

clean_log(logdir)

print "Starting service nodes..."

for node in service_nodes:
    javaservicecmd = "%s -jar %s %s" % (javaCommand, cfabcastjar, node)
    localcmdbg(javaservicecmd)
    sleep(2)
