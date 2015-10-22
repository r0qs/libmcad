#!/usr/bin/python

import benchCommon
import time

user = benchCommon.sarg(1)

# cleaning remote nodes
for node in benchCommon.availableNodes :
    # print "Cleaning worker " + node + "..."
    benchCommon.sshcmdbg(node, "killall -9 -u "+ user +" &> /dev/null")
    # user bezerrac1 or bezerrac?
    benchCommon.localcmdbg("ssh "+ user +"@node249 ssh " + node + " sudo killall -9 -u "+ user +" &> /dev/null")

time.sleep(5)
