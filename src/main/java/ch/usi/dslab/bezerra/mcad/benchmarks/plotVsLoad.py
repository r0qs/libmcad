#!/usr/bin/python

import glob, os, re, sys
from benchCommon import *

gnuplot_script_sh_path=HOME + "/libmcad/benchLink/simple_tplat.sh"

####################################################################################################
# functions
####################################################################################################
def add(l, v) :
    if v not in l :
        l.append(v)

def ds(d, n) :
    return re.split("_", d)[n]

def getAllValsFromDirs(pattern, position) :
    allVals = []
    alldirs = glob.glob(pattern)
    for d in alldirs :
        if "overall" in d : continue
        val = ds(d, position)
        add(allVals, val)
    return sorted(allVals)

def getDirectoryPattern(algorithm="*", clients="*", learners="*", groups="*", pxpg="*", size="*", wtodisk="*") :
    return ("%s_%s_clients_%s_learners_%s_groups_%s_pxpergroup_%s_bytes_diskwrite_%s" \
            % (algorithm, clients, learners, groups, pxpg, size, wtodisk))

def getfileline(name, linenum) :
    linenum -= 1
    fp = None
    try :
        fp = open(name)
    except IOError :
        print "Error: couldn't find %s. Skipping. " % (name)
        return None
    wantedline = ""
    for i, line in enumerate(fp):
        if i == linenum:
            wantedline = line
            break
    fp.close()
    return wantedline

def getAvgLatency(d) :
    l = getfileline(d + "/latency_conservative_average.log", 3)
    return l.split()[2] if l != None else None

def getAggThroughput(d) :
    l = getfileline(d + "/throughput_conservative_aggregate.log", 3)
    return l.split()[2] if l != None else None

def saveToFile(filepath, pointlist) :
    # p is a tuple (x,y)
    f = open(filepath, "w")
    for p in pointlist :
        f.write("%s %s\n" % p)
    f.close()

def plot(dirPath,msgSize) :
    print "Plotting in directory %s" % dirPath
    os.system("%s %s %s"   % (gnuplot_script_sh_path,dirPath,msgSize))

def generate_max_and_75_tp_lat_files(allLatencies, allThroughputs, overall_dir_name) :
    file_max = overall_dir_name + "/tp_lat_max.log"
    file_75  = overall_dir_name + "/tp_lat_75.log"
    load_max = tp_max = lat_max = None
####################################################################################################
####################################################################################################



####################################################################################################
# main code
####################################################################################################
doPlotting = False
if len(sys.argv) > 1 :
    doPlotting = sys.argv[1] in ["True","true","T","t","1"]

# libpaxos_10_clients_16_learners_1_groups_1_pxpergroup_140_bytes_diskwrite_False
alldirs = "*_clients_*learners*groups*pxpergroup*"

all_algs     = getAllValsFromDirs(alldirs,  0)
all_clis     = getAllValsFromDirs(alldirs,  1)
all_learners = getAllValsFromDirs(alldirs,  3)
all_groups   = getAllValsFromDirs(alldirs,  5)
all_pxpg     = getAllValsFromDirs(alldirs,  7)
all_sizes    = getAllValsFromDirs(alldirs,  9)
all_disks    = getAllValsFromDirs(alldirs, 12)

for alg in all_algs :
    for learners in all_learners :
        for groups in all_groups :
            for pxpg in all_pxpg :
                for size in all_sizes :
                    for wdisk in all_disks :
                        overall_dir_name = "overall_" + getDirectoryPattern(alg, "all", learners, groups, pxpg, size, wdisk)
                        dirpattern = getDirectoryPattern(alg, "*", learners, groups, pxpg, size, wdisk)
                        clis = sorted([int(v) for v in getAllValsFromDirs(dirpattern, 1)])
                        if len(clis) == 0 :
                            continue
                        allLatencies   = []
                        allThroughputs = []
                        for cli in clis :
                            cliDir = getDirectoryPattern(alg, cli, learners, groups, pxpg, size, wdisk)
                            latency = getAvgLatency(cliDir)
                            throughput = getAggThroughput(cliDir)
                            if latency == None or throughput == None:
                                continue
                            allLatencies  .append((cli, latency))
                            allThroughputs.append((cli, throughput))
                        if not allLatencies or not allThroughputs :
                            print "No valid points for %s. Skipping." % (overall_dir_name)
                            continue

                        overall_latency_file    = overall_dir_name + "/latency.log"
                        overall_throughput_file = overall_dir_name + "/throughput.log"
                        overall_max_tp_file = overall_dir_name + "/tp_lat_max.log"
                        overall_75_tp_file = overall_dir_name + "/tp_lat_75.log"
                        if not os.path.exists(overall_dir_name) :
                            os.makedirs(overall_dir_name)
                        saveToFile(overall_latency_file,    allLatencies)
                        saveToFile(overall_throughput_file, allThroughputs)
                        if doPlotting :
                            plot(overall_dir_name, size)
                        
                        generate_max_and_75_tp_lat_files(allLatencies, allThroughputs, overall_dir_name)