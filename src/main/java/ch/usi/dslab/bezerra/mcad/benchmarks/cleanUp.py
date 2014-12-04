#!/usr/bin/python

import benchCommon

# cleaning remote nodes
for node in benchCommon.availableNodes + ["node40"] :
    # print "Cleaning worker " + node + "..."
    benchCommon.sshcmdbg(node, "killall -9 -u bezerrac &> /dev/null")
    benchCommon.localcmd("ssh bezerrac1@node249 ssh " + node + " killall -9 -u bezerrac &> /dev/null &")
