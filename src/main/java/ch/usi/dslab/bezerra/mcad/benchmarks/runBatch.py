from benchCommon import localcmd
import sys
import time
import benchCommon

numClients = sys.argv[1]
clientId = 9
from os.path import expanduser
HOME = expanduser("~")

clientNodes = ["node"+str(nid) for nid in range(9, 40 + 1)]

deployer = HOME + "/libmcad/src/main/java/ch/usi/dslab/bezerra/mcad/ridge/RidgeEnsembleNodesDeployer.py"
config = HOME + "/libmcad/src/main/java/benchmark/ridge_config.json"
localcmd(deployer + " " + config)

serverClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchServer "
clientClass = "ch.usi.dslab.bezerra.mcad.benchmarks.BenchClient "
gathererClass="ch.usi.dslab.bezerra.sense.DataGatherer "

javaservercmd = "java -XX:+UseG1GC -Xmx8g -cp " + HOME + "/libmcad/target/libmcad-git.jar " + serverClass
benchCommon.sshcmdbg("node7", javaservercmd + "7 " + config)
benchCommon.sshcmdbg("node8", javaservercmd + "8 " + config)

time.sleep(5)

# DataGatherer:
#             0         1           2          3         4
# <command> <port> <directory> {<resource> <nodetype> <count>}+

javagatherercmd  = "java -XX:+UseG1GC -Xmx8g -cp " + HOME + "/libmcad/target/libmcad-git.jar " + gathererClass
javagatherercmd += " 60000 " + "/home/bezerrac/logsRidge/clients_" + str(numClients)
javagatherercmd += " latency conservative "    + str(numClients)
javagatherercmd += " latency optimistic "      + str(numClients)
javagatherercmd += " throughput conservative " + str(numClients)
javagatherercmd += " throughput optimistic "   + str(numClients)

benchCommon.sshcmdbg("node41", javagatherercmd)

javacliendcmd = "java -XX:+UseG1GC -Xmx8g -cp " + HOME + "/libmcad/target/libmcad-git.jar " + clientClass

while numClients > 0 :
    for clinode in clientNodes :
        benchCommon.sshcmdbg(clinode, javacliendcmd + str(clientId) + " " + config + " 100")
        clientId += 1
        numClients -= 1
        if numClients <= 0 :
            break