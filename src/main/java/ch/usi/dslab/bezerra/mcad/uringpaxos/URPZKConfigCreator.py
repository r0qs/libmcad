#!/usr/bin/python

import sys
import os
import shlex
import subprocess

def sshcmd(node, cmdstring) :
    os.cmd("ssh " + node + " " + cmdstring)

def sshcmdbg(node, cmdstring) :
    run_args = shlex.split("ssh " + node + " " + cmdstring)
    print("xXx ssh " + node + " " + cmdstring)
    return subprocess.Popen(run_args)

# arg1: zknode
# arg2: zkport
# arg3: zkpath

zknode = sys.argv[1]
zkport = sys.argv[2]
zkpath = sys.argv[3]


sshcmd(zknode, zkpath + " stop")
sshcmd(zknode, zkpath + " start")