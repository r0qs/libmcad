#!/usr/bin/python
# -*- coding: utf-8 -*-

import os, sys, shlex, threading, subprocess
from time import sleep

def clean_log(logdir) :
    assert logdir.strip() != " "
    localcmd("rm -rf " + logdir + "/*")
    # service logs
    localcmd("rm -rf logs/*.log target/snapshots/ target/shared-journal/")

# available machines
def noderange(first,last) :
    return ["node" + str(val) for val in range(first, last + 1)]

def testNodes() :
    for n in availableNodes :
        exitcode = localcmd("ssh %s uname -a" % (n))
        assert exitcode == 0

# cluster available nodes
availableNodes = noderange(42,45)

class NodePool:
    nodePointer = -1
    nodes = availableNodes
    def last(self):
        assert self.nodePointer in range(len(self.nodes))
        return self.nodes[self.nodePointer]
    def next(self) :
        self.nodePointer += 1
        return self.last()
    def checkSize(self, num) :
        assert num <= len(self.nodes)
    def all(self) :
        return list(self.nodes)
    def nextn(self, n):
        ret = []
        for _ in range (n) :
            ret.append(self.next())
        return ret

class Command(object):
    def __init__(self, cmd):
        self.cmd = cmd
        self.process = None

    def run(self, timeout):
        def target():
            print 'Thread started'
            run_args = shlex.split(self.cmd)
            self.process = subprocess.Popen(run_args)
            self.process.communicate()
            print 'Thread finished'

        thread = threading.Thread(target=target)
        thread.start()

        thread.join(timeout)
        if thread.is_alive():
            print 'Terminating process'
            self.process.terminate()
            thread.join()
        return self.process.returncode

def localcmd(cmdstring, timeout=None) :
    print "localcmd: " + cmdstring
    cmd = Command(cmdstring)
    return cmd.run(timeout)

def localcmdbg(cmdstring, env="") :
    print "localcmdbg: " + cmdstring
    os.system(env + " ; " + cmdstring + " &")

def sshcmd(node, cmdstring, timeout=None) :
    finalstring = "ssh " + node + " \"" + cmdstring + "\""
    print finalstring
    cmd = Command(finalstring)
    return cmd.run(timeout)
    
def sshcmdbg(node, cmdstring, env="") :
    print "ssh " + node + " \'" + env + " " + cmdstring + "\' &"
    os.system("ssh " + node + " \'" + env + " " + cmdstring + "\' &")

#    ssh -t node42 'export MYVAR="extra information"; export OLA="eita"; echo $MYVAR ; echo $OLA'

# constants
NODE = 0
CLIENTS = 1

# clientMap is a list of dicts
# clientMap = [{NODE: x, CLIENTS: y}, {NODE: z, CLIENTS: w}]
def mapClientsToNodes(numClients, nodesList) :
    clientMap = []
    clientsPerNode = int(numClients/len(nodesList))
    for node in nodesList :
        clientMap.append({NODE: node, CLIENTS: clientsPerNode})
    for extra in range(numClients % len(nodesList)) :
        clientMap[extra][CLIENTS] += 1
    return clientMap

# clientMap is a list of dicts
# clientMap = [{NODE: x, CLIENTS: y}, {NODE: z, CLIENTS: w}]
def clientNodeIsEmpty(node, clientMap) :
    for mapping in clientMap :
        if mapping[NODE] == node and mapping[CLIENTS] > 0 :
            return False
    return True

def createIdPerNodeList(nodes, firstId = 0):
    nodeList = []
    for id in range(firstId, len(nodes)) :
        node = {"id": id, "host" : nodes[id]}
        nodeList.append(node)
    return nodeList
