#!/usr/bin/python

############### THREAD (to make things bearably fast)

import threading
import benchCommon
from benchCommon import sshcmd

lock = threading.Lock()
 
class paramSetterThread(threading.Thread):
    def __init__(self, target, *args):
        self._target = target
        self._args = args
        threading.Thread.__init__(self)
 
    def run(self):
        self._target(*self._args)

#####################################################

def setSystemParameters(node) :
    # get nid from node string: 
    nid = benchCommon.getNid(node)
    if nid <= 40 :
        irq = "29"
        mask = "ff"
    else :
        irq = "80"
        mask = "f"
    
    lock.acquire()
    print "setting parameters in " + node + "..."
    lock.release()

    sshcmd(node, "sudo bash -c \'sudo service irqbalance stop\' ; sudo bash -c \'sudo echo " + mask + " > /proc/irq/" + irq + "/smp_affinity'")
    
    sshcmd(node, "sudo sysctl -w net.core.rmem_max=17000000; sudo sysctl -w net.core.rmem_default=17000000;")
    sshcmd(node, "sudo sysctl -w net.core.wmem_max=17000000; sudo sysctl -w net.core.wmem_default=17000000;")

    sshcmd(node, "sudo sysctl -w net.core.wmem_max=4194304")
    sshcmd(node, "sudo sysctl -w net.core.rmem_max=4194304")
    sshcmd(node, "sudo sysctl -w net.core.wmem_default=4194304")
    sshcmd(node, "sudo sysctl -w net.core.rmem_default=4194304")
    sshcmd(node, "sudo sysctl -w net.core.optmem_max=20480")
    sshcmd(node, "sudo sysctl -w net.ipv4.igmp_max_memberships=20")
    sshcmd(node, "sudo sysctl -w net.ipv4.tcp_mem=\'4194304 4194304 4194304\'")
    sshcmd(node, "sudo sysctl -w net.ipv4.tcp_wmem=\'4194304 4194304 4194304\'")
    sshcmd(node, "sudo sysctl -w net.ipv4.tcp_rmem=\'4194304 4194304 4194304\'")
    sshcmd(node, "sudo sysctl -w net.ipv4.udp_mem=\'4194304 4194304 4194304\'")
    sshcmd(node, "sudo sysctl -w net.ipv4.udp_rmem_min=4096")
    sshcmd(node, "sudo sysctl -w net.ipv4.udp_wmem_min=4096")
    sshcmd(node, "sudo sysctl -w net.core.netdev_max_backlog=1048576")
    
    # For our network cards, I would add this (it enables all offloading option that our cards support):
    
    sshcmd(node, "sudo ethtool -K eth0 rx on tx on sg on tso on gso on")
    
    # To balance the load of receiving messages (and receive in all cores)
    
    sshcmd(node, "sudo bash -c \'sudo service irqbalance stop\'; sudo bash -c \'sudo echo " + mask + " > /proc/irq/" + irq + "/smp_affinity\'")
    
    lock.acquire()
    print "done with " + node + "."
    lock.release()

nodesToConfigure = benchCommon.availableNodes

threads = []

for node in nodesToConfigure :
    t = paramSetterThread(setSystemParameters, node)
    threads.append(t)
    t.start()

for t in threads :
    t.join()
