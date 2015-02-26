#!/usr/bin/python

import benchCommon
import time

# cleaning remote nodes
for node in benchCommon.availableNodes :
    # print "Cleaning worker " + node + "..."
    benchCommon.sshcmdbg(node, "killall -9 -u bezerrac &> /dev/null")
    benchCommon.localcmdbg("ssh bezerrac1@node249 ssh " + node + " sudo killall -9 -u bezerrac &> /dev/null")

time.sleep(5)
