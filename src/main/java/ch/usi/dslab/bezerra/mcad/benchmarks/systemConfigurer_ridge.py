#!/usr/bin/python

# try: import simplejson as json
# except ImportError: import json

import simplejson as json

from benchCommon import *

class RidgeConfiguration :
    config             = None
    configFilePath     = None
    coordinator_list   = None
    acceptor_list      = None
    server_list        = None
    client_initial_pid = None
    remaining_nodes    = None
    gathererNode       = None
    partitionsFilePath = None

def containsCoordinator(sequence) :
    for process in sequence :
        if process["role"] == "coordinator" :
            return True
    return False

def getCoordinator(ensemble) :
    for process in ensemble :
        if process["role"] == "coordinator" :
            return process
    return None

def addCoordinator(sequence, ensemble) :
    coordinator = getCoordinator(ensemble)
    sequence.insert(0, coordinator)

serverList = [{"id": 0, "partition": 0}]

# ridgeConfiguration = generateRidgeConfiguration(availableNodes, numPartitions, replicasPerPartition, ensembleSize, configFilePath)

def generateRidgeConfiguration(nodes, numGroups, numPxPerGroup, numLearners, ensembleSize, writeToDisk, configFilePath, saveToFile) :
    config = dict()
    config["agent_class"] = "RidgeMulticastAgent"
    if writeToDisk == True :
        config["delta_null_messages_ms"] = delta_null_messages_ms_disk
        config["storage_type"]           = ridge_disk_storage_type
        config["batch_size_threshold_bytes"] = batch_size_threshold_bytes_disk
        config["batch_time_threshold_ms"]    = batch_time_threshold_ms_disk
    else :
        config["delta_null_messages_ms"] = delta_null_messages_ms_memory
        config["storage_type"]           = ridge_memory_storage_type
        config["batch_size_threshold_bytes"] = batch_size_threshold_bytes_memory
        config["batch_time_threshold_ms"]    = batch_time_threshold_ms_memory
    config["deliver_conservative"]       = True
    config["deliver_optimistic_uniform"] = False
    config["deliver_optimistic_fast"]    = False
    config["direct_fast"]                = True
    config["latency_estimation_sample"]  = latency_estimation_sample
    config["latency_estimation_devs"]    = latency_estimation_devs
    config["latency_estimation_max"]     = latency_estimation_max
    
    # OKAY
    # groups (1:1 with partitions)
    config["groups"] = []
    for gid in range(1, numGroups + 1) :
        config["groups"].append({"group_id" : gid})
    
    # OKAY, assume max 10 px per group
    # ensembles
    config["ensembles"] = []
    ensembleRange = None
    if numGroups == 1 :
        ensembleRange = [10 + pxid for pxid in range(numPxPerGroup)]
    else:
        ensembleRange = [10*g + pxid for g in range(numGroups + 1) for pxid in range(numPxPerGroup)]
    numEnsembles = len(ensembleRange)
    for e in ensembleRange :
        if (e < 10) : destination_groups = range(1, numGroups + 1)
        else        : destination_groups = [e//10]
        ensemble = {"ensemble_id" : e, "learner_broadcast_mode" : "DYNAMIC", "destination_groups" : destination_groups}
        config["ensembles"].append(ensemble)
    
    # OKAY
    # helper processes (neither servers nor clients)
    helperList = {"coordinator": [], "acceptor": []}
    firstServerPid = 0
    allEnsembles = dict()
    config["ensemble_processes"] = []
    quorumSize = int(ensembleSize / 2) + 1
    # numDeployedPerEnsemble = ensembleSize
    numDeployedPerEnsemble = quorumSize # in practice, only the quorum nodes need to be deployed
    
    numRequiredNodes = numDeployedPerEnsemble * numEnsembles + numLearners
    if  numRequiredNodes > len(nodes) :
        print "Not enough nodes: have %s, need %s" % (len(nodes), numRequiredNodes)
        return None
    
    numLearnersPerGroup = numLearners // numGroups
    
    for pid in range(0, len(nodes), numDeployedPerEnsemble) :
        if pid >= numDeployedPerEnsemble * numEnsembles : 
            firstServerPid = pid
            break
        eid = ensembleRange[int(pid / numDeployedPerEnsemble)]
        allEnsembles[eid] = []
        for j in range(numDeployedPerEnsemble) :
            process = {"pid"  : pid + j, "ensemble" : eid, "host" : nodes[pid + j], "port" : 50000 + pid + j}
            if j == 0 : process["role"] = "coordinator"
            else      : process["role"] = "acceptor"
            config["ensemble_processes"].append(process)
            allEnsembles[eid].append(process)
            helperList[process["role"]].append(process)
    
    # OKAY... but too sophisticated and we're not really writing to disk
    # acceptor sequences v2.0
#     config["acceptor_sequences"] = []
#     quorumSize = int(ensembleSize / 2) + 1
#     seqId = 0
#     for eid in allEnsembles :
#         ensemble = allEnsembles[eid]
#         allSequences = [list(seq) for seq in itertools.combinations(ensemble, quorumSize)]
#         for sequence in allSequences :
#             coordinator_writes = True
#             if not containsCoordinator(sequence) :
#                 addCoordinator(sequence, ensemble)
#                 coordinator_writes = False
#             onlyAccIds = [process["pid"] for process in sequence]
#             formattedSequence = {"id" : seqId, "ensemble_id" : eid, "coordinator_writes" : coordinator_writes, "acceptors" : onlyAccIds}
#             seqId +=1
#             config["acceptor_sequences"].append(formattedSequence)
    
    # OKAY... this is the simple version that creates a single acceptor sequence per ensemble
    # acceptor sequences v1.0
    config["acceptor_sequences"] = []
    quorumSize = int(ensembleSize / 2) + 1
    seqId = 0
    for eid in allEnsembles :
        ensemble = allEnsembles[eid]
        sequence = ensemble[:quorumSize]
        onlyAccIds = [process["pid"] for process in sequence]
        formattedSequence = {"id" : seqId, "ensemble_id" : eid, "coordinator_writes" : True, "acceptors" : onlyAccIds}
        seqId +=1
        config["acceptor_sequences"].append(formattedSequence)
    
    # learners
    remainingNodes = None
    minClientId = 0
    serverList = []
    config["group_members"] = []
    for sid in range(firstServerPid, len(nodes)) :
        gid = 1 + int((sid - firstServerPid) / numLearnersPerGroup)
        if gid > numGroups :
            minClientId = sid
            remainingNodes = nodes[sid:]
            break
        learner = {"pid" : sid, "group" : gid, "host" : nodes[sid], "port" : 50000 + sid}
        config["group_members"].append(learner)
        server = {"id": sid, "partition": gid, "host" : nodes[sid], "pid" : sid, "role" : "server"}
        serverList.append(server)
    
    if saveToFile :
        systemConfigurationFile = open(configFilePath, 'w')
        json.dump(config, systemConfigurationFile, sort_keys = False, indent = 4, ensure_ascii=False)
        systemConfigurationFile.flush()
        systemConfigurationFile.close()
    
    ridgeConfiguration = RidgeConfiguration()
    
    ridgeConfiguration.config             = config
    ridgeConfiguration.configFilePath     = configFilePath
    ridgeConfiguration.coordinator_list   = helperList["coordinator"]
    ridgeConfiguration.acceptor_list      = helperList["acceptor"]
    ridgeConfiguration.server_list        = serverList
    ridgeConfiguration.client_initial_pid = minClientId
    ridgeConfiguration.remaining_nodes    = remainingNodes
        
    # ridgeConfiguration = {
    #                       "config_file": configFilePath,
    #                       "coordinator_list": helperList["coordinator"],
    #                       "acceptor_list": helperList["acceptor"],
    #                       "server_list": serverList,
    #                       "client_initial_pid": minClientId,
    #                       "remaining_nodes": remainingNodes,
    #                       }
    
    return ridgeConfiguration 

def generatePartitioningFile(serverList, partitionsFile, saveToFile) :
    pconf = dict()
    pconf["partitions"] = []
    for s in serverList :
        pentry = get_item(pconf["partitions"], "id", s["partition"]) 
        if pentry == None :
            pentry = {"id": s["partition"], "servers": []}
            pconf["partitions"].append(pentry)
        pentry["servers"].append(s["id"])
        
    if saveToFile :
        partitioningFile = open(partitionsFile, 'w')
        json.dump(pconf, partitioningFile, sort_keys = False, indent = 4, ensure_ascii=False)
        partitioningFile.flush()
        partitioningFile.close()
        
    return pconf
        

def generateRidgeSystemConfiguration(nodes, numGroups, numPxPerGroup, numLearnersPerGroup, ensembleSize, writeToDisk, ensemblesFilePath, partitionsFilePath, saveToFile = True) :
    gathererNode = nodes[0]
    remainingNodes = availableNodes[1:]
    
    systemConfiguration = generateRidgeConfiguration(remainingNodes, numGroups, numPxPerGroup, numLearnersPerGroup, ensembleSize, writeToDisk, ensemblesFilePath, saveToFile)
    if systemConfiguration == None :
        return None
    generatePartitioningFile(systemConfiguration.server_list, partitionsFilePath, saveToFile)
    
    systemConfiguration.gathererNode       = gathererNode
    systemConfiguration.partitionsFilePath = partitionsFilePath
#     configuration = {"config_file": configFilePath, "partitioning_file": None, "server_list": serverList, "client_initial_pid": minClientId}
    return systemConfiguration

def getGathererNode(sysConfig) :
    return sysConfig["gatherer_node"]

def getClientNodes(sysConfig) :
    return sysConfig["remaining_nodes"]


# example
# print generateSystemConfiguration(2)
# returned:
# {'client_initial_pid': 13, 'partitioning_file': '/Users/eduardo/chirper/src/main/java/ch/usi/dslab/bezerra/chirper/benchmarks/generatedPartitionsConfig.json', 'config_file': '/Users/eduardo/chirper/src/main/java/ch/usi/dslab/bezerra/chirper/benchmarks/generatedSysConfig.json', 'server_list': [{'partition': 1, 'id': 9}, {'partition': 1, 'id': 10}, {'partition': 2, 'id': 11}, {'partition': 2, 'id': 12}]}
    
############################
# based on:
###########



# PARTITIONING FILE:

#################################################
# {"partitions" : [{"id" : 1 , "servers" : [9]} ,
#                  {"id" : 2 , "servers" : [10]}
#                 ]
# }
#################################################



# SYSTEM CONFIG FILE:

#################################################
# {
#   "agent_class" : "RidgeMulticastAgent" ,
#   
#   "batch_size_threshold_bytes" : 30 ,
#   "batch_time_threshold_ms"    : 5 ,
#   "delta_null_messages_ms"     : 1 ,
#   
#   "deliver_conservative"       : true ,
#   "deliver_optimistic_uniform" : false ,
#   "deliver_optimistic_fast"    : true ,
#   "direct_fast"                : true ,
#   
#   "latency_estimation_sample"  : 5 ,
#   "latency_estimation_devs"    : 0 ,
#   "latency_estimation_max"     : 10 ,
#   
#   "groups" :
#   [
#     {
#       "group_id" : 1
#     } ,
#     {
#       "group_id" : 2
#     }
#   ] ,
#   
#   "ensembles" :
#   [
#     {
#       "ensemble_id" : 0 ,
#       "learner_broadcast_mode" : "DYNAMIC",
#       "destination_groups" : [1]
#     } ,
#     {
#       "ensemble_id" : 1 ,
#       "learner_broadcast_mode" : "DYNAMIC",
#       "destination_groups" : [2]
#     } ,
#     {
#       "ensemble_id" : 2 ,
#       "learner_broadcast_mode" : "DYNAMIC",
#       "destination_groups" : [1, 2]
#     }
#   ] ,
#   
#   "ensemble_processes" :  
#   [
#     {
#       "role" : "coordinator",
#       "pid"  : 0,
#       "ensemble" : 0,
#       "host" : "localhost",
#       "port" : 50000
#     },
#     {
#       "role" : "acceptor",
#       "pid"  : 1,
#       "ensemble" : 0,
#       "host" : "localhost",
#       "port" : 50001
#     },
#     {
#       "role" : "acceptor",
#       "pid"  : 2,
#       "ensemble" : 0,
#       "host" : "localhost",
#       "port" : 50002
#     },
#     {
#       "role" : "coordinator",
#       "pid"  : 3,
#       "ensemble" : 1,
#       "host" : "localhost",
#       "port" : 50003
#     },
#     {
#       "role" : "acceptor",
#       "pid"  : 4,
#       "ensemble" : 1,
#       "host" : "localhost",
#       "port" : 50004
#     },
#     {
#       "role" : "acceptor",
#       "pid"  : 5,
#       "ensemble" : 1,
#       "host" : "localhost",
#       "port" : 50005
#     },
#     {
#       "role" : "coordinator",
#       "pid"  : 6,
#       "ensemble" : 2,
#       "host" : "localhost",
#       "port" : 50006
#     },
#     {
#       "role" : "acceptor",
#       "pid"  : 7,
#       "ensemble" : 2,
#       "host" : "localhost",
#       "port" : 50007
#     },
#     {
#       "role" : "acceptor",
#       "pid"  : 8,
#       "ensemble" : 2,
#       "host" : "localhost",
#       "port" : 50008
#     }
#   ],
#   
#   "acceptor_sequences" :
#   [
#     {
#       "id" : 0,
#       "ensemble_id" : 0,
#       "coordinator_writes" : true,
#       "acceptors" : [0, 1]
#     },
#     {
#       "id" : 1,
#       "ensemble_id" : 1,
#       "coordinator_writes" : true,
#       "acceptors" : [3, 4]
#     },
#     {
#       "id" : 2,
#       "ensemble_id" : 2,
#       "coordinator_writes" : true,
#       "acceptors" : [6, 7]
#     }
#   ],
#   
#   "group_members" :
#   [
#     {
#       "pid"   : 9,
#       "group" : 1,
#       "host"  : "localhost",
#       "port"  : 50009
#     },
#     {
#       "pid"   : 10,
#       "group" : 2,
#       "host"  : "localhost",
#       "port"  : 50010
#     }
#   ]
#   
# }
#################################################
