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
    return float(l.split()[2]) if l != None else None

def getAggThroughput(d) :
    l = getfileline(d + "/throughput_conservative_aggregate.log", 3)
    return float(l.split()[2]) if l != None else None

def saveToFile(filepath, pointlist) :
    # p is a tuple (x,y)
    f = open(filepath, "w")
    for p in pointlist :
        f.write("%s %s\n" % p)
    f.close()

def plot(dirPath, msgSize, lat_max_avg) :
    print "Plotting in directory %s" % dirPath
    os.system("%s %s %s %s"   % (gnuplot_script_sh_path,dirPath,msgSize,lat_max_avg))

def create_cdf_plot(line_cdf_file, plot_file_name) :
    lineCdf = getfileline(line_cdf_file, 3)
    lineCdfElements = lineCdf.split()
    properCdf = []
    totalCount = 0
    for i in range(1,len(lineCdfElements),2) :
        x = int  (lineCdfElements[i  ]) # bucketRange
        y = float(lineCdfElements[i+1]) # bucketCount
        properCdf.append((x,y))
        totalCount = y
    properCdfFile = open(plot_file_name, "w")
    for point in properCdf :
        properCdfFile.write("%s %s\n" % (point[0], point[1]/totalCount))
    properCdfFile.close()

def generate_max_and_75_tp_lat_files(allThroughputsLatencies, msgSize, overall_dir_name, alg, learners, groups, pxpg, sizeStr, wdisk) :
    load_max = tp_max = lat_max = 0
    for point in allThroughputsLatencies :
        load,tp,lat = point
        if tp > tp_max :
            load_max = load
            tp_max   = tp
            lat_max  = lat

    tp_75_ideal = 0.75*tp_max
    load_75_estimate = int(round(0.75*load_max))
    tp_75_found = load_75_found = lat_75_found = 0
    lowest_distance = float("inf")
    for point in allThroughputsLatencies :
        load,tp,lat = point
        distance = abs(tp - tp_75_ideal)
        if distance < lowest_distance : 
            lowest_distance = distance
            load_75_found   = load
            tp_75_found     = tp
            lat_75_found    = lat

    # max
    file_max = open(overall_dir_name + "/tp_lat_max.log", "w")
    file_max.write("# maxtp = %s (load %s), ideal 0.75maxtp = %s (estimated load %s)\n" % (tp_max, load_max, tp_75_ideal, load_75_estimate))
    file_max.write("# load throughput(msg/s) throughput(MBps) latency(ns):\n")
    file_max.write("%s %s %s %s\n" % (load_max, tp_max, tp_max*msgSize*8/1e6, lat_max))
    file_max.close()
    
    # 75 found
    tp_distance_percent   = 100*abs(tp_75_found   - tp_75_ideal     )/tp_75_ideal
    load_distance_percent = 100*abs(load_75_found - load_75_estimate)/load_75_estimate
    file_75 = open(overall_dir_name + "/tp_lat_75.log",  "w")
    file_75.write("# maxtp = %s (load %s), ideal 0.75maxtp = %s (estimated load %s), found 0.75maxtp = %s (load %s): distance %s%% (load distance %s%%)\n" \
                  % (tp_max, load_max, tp_75_ideal, load_75_estimate, tp_75_found, load_75_found, tp_distance_percent, load_distance_percent))
    file_75.write("# load throughput(msg/s) throughput(MBps) latency(ns):\n")
    file_75.write("%s %s %s %s\n" % (load_75_found, tp_75_found, tp_75_found*msgSize*8/1e6, lat_75_found))
    file_75.close()
    
    #latency cdf load_tp_max
    load_max_directory = getDirectoryPattern(alg, load_max, learners, groups, pxpg, size, wdisk)
    cdf_max_file = load_max_directory + "/latencydistribution_conservative_aggregate.log"
    create_cdf_plot(cdf_max_file, overall_dir_name + "/cdf_max.log")
    
    # latency cdf load_tp_75
    load_75_directory = getDirectoryPattern(alg, load_75_found, learners, groups, pxpg, size, wdisk)
    cdf_75_file = load_75_directory + "/latencydistribution_conservative_aggregate.log"
    create_cdf_plot(cdf_75_file, overall_dir_name + "/cdf_75.log")
    
    return lat_max
    
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
                        allThroughputsLatencies = []
                        for cli in clis :
                            cliDir = getDirectoryPattern(alg, cli, learners, groups, pxpg, size, wdisk)
                            latency = getAvgLatency(cliDir)
                            throughput = getAggThroughput(cliDir)
                            if latency == None or throughput == None:
                                continue
                            allLatencies  .append((cli, latency))
                            allThroughputs.append((cli, throughput))
                            allThroughputsLatencies.append((cli, throughput, latency))
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
                        lat_max_avg = generate_max_and_75_tp_lat_files(allThroughputsLatencies, int(size), overall_dir_name, alg, learners, groups, pxpg, size, wdisk)
                        if doPlotting :
                            plot(overall_dir_name, size, lat_max_avg)
                        
