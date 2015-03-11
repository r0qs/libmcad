#!/usr/bin/python

import glob, os, re, sys
from benchCommon import *

####################################################################################################
# definition
####################################################################################################
simple_gnuplot_script_sh_path=HOME + "/libmcad/benchLink/plot_simple_tplat_cdf.sh"
broadcast_gnuplot_sh_path=HOME + "/libmcad/benchLink/plot_broadcast.sh"
prettynames = {"libpaxos" : "LibPaxos",
               "ridge"    : "Ridge"   ,
               "mrp"      : "\"Ring Paxos\"",
               "spread"   : "Spread"}

####################################################################################################
# functions
####################################################################################################
def add(l, v) :
    if v not in l :
        l.append(v)

def ds(d, n) :
    return re.split("_", d)[n]

def getAllValsFromDirs(pattern, position) :
#     print "pattern = %s, position = %s" % (pattern, position)
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

def getfilelinecolum(name, linenum, colnum) :
    line = getfileline(name, linenum)
    return line.split()[colnum - 1] if line != None else 0

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

def plot_overalls(dirPath, msgSize, lat_max_avg) :
    print "Plotting in directory %s" % dirPath
    os.system("%s %s %s %s" % (simple_gnuplot_script_sh_path, dirPath, msgSize, lat_max_avg))

def plot_broadcast(sizes) :
    for size in sizes :
        print "Plotting broadcast graphs for %s bytes" % (size)
        os.system("%s %s %s" % (broadcast_gnuplot_sh_path, os.getcwd() + "/../", size))

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
    latency_50th = None
    latency_95th = None
    latency_99th = None
    for point in properCdf :
        latency_bucket = point[0]
        percentile = point[1]/totalCount
        properCdfFile.write("%s %s\n" % (latency_bucket, percentile))
        if latency_50th == None and percentile >= 0.50 :
            latency_50th = latency_bucket
        if latency_95th == None and percentile >= 0.95 :
            latency_95th = latency_bucket
        if latency_99th == None and percentile >= 0.99 :
            latency_99th = latency_bucket
    properCdfFile.close()
    return (latency_50th,latency_95th,latency_99th)

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

    #latency cdf load_tp_max
    load_max_directory = getDirectoryPattern(alg, load_max, learners, groups, pxpg, size, wdisk)
    cdf_max_file = load_max_directory + "/latencydistribution_conservative_aggregate.log"
    latency_50th_max,latency_95th_max,latency_99th_max = create_cdf_plot(cdf_max_file, overall_dir_name + "/cdf_max.log")
    
    # latency cdf load_tp_75
    load_75_directory = getDirectoryPattern(alg, load_75_found, learners, groups, pxpg, size, wdisk)
    cdf_75_file = load_75_directory + "/latencydistribution_conservative_aggregate.log"
    latency_50th_75,latency_95th_75,latency_99th_75 = create_cdf_plot(cdf_75_file, overall_dir_name + "/cdf_75.log")
    
    # max file
    file_max = open(overall_dir_name + "/tp_lat_max.log", "w")
    file_max.write("# maxtp = %s (load %s), ideal 0.75maxtp = %s (estimated load %s)\n" % (tp_max, load_max, tp_75_ideal, load_75_estimate))
    file_max.write("# load throughput(msg/s) throughput(MBps) latency_avg(ms) latency_95th(ms) latency_99th(ms):\n")
    file_max.write("%s %s %s %s %s %s %s\n" % (load_max, tp_max, tp_max*msgSize*8/1e6, lat_max/1e6, latency_50th_max/1e6, latency_95th_max/1e6, latency_99th_max/1e6))
    file_max.close()
    
    # 75 found file
    tp_distance_percent   = 100*abs(tp_75_found   - tp_75_ideal     )/tp_75_ideal
    load_distance_percent = 100*abs(load_75_found - load_75_estimate)/load_75_estimate
    file_75 = open(overall_dir_name + "/tp_lat_75.log",  "w")
    file_75.write("# maxtp = %s (load %s), ideal 0.75maxtp = %s (estimated load %s), found 0.75maxtp = %s (load %s): distance %s%% (load distance %s%%)\n" \
                  % (tp_max, load_max, tp_75_ideal, load_75_estimate, tp_75_found, load_75_found, int(round(tp_distance_percent)), int(round(load_distance_percent))))
    file_75.write("# load throughput(msg/s) throughput(MBps) latency_avg(ms) latency_95th(ms) latency_99th(ms):\n")
    file_75.write("%s %s %s %s %s %s %s\n" % (load_75_found, tp_75_found, tp_75_found*msgSize*8/1e6, lat_75_found/1e6, latency_50th_75/1e6, latency_95th_75/1e6, latency_99th_75/1e6))
    file_75.close()
    
    return latency_50th_max

def createBroadcastData() :
    # assuming that the script is run inside logsmcast/algorithm/
    # create a table of (alg,learners,msgsize) -> (maxtpmsgps,maxtpmbps,lat_avg,lat_50,lat_95)
    
    # how to create a file for a clustered histogram in gnuplot
    # 
    #
#
#   key: group1: AEI
#        group2: BFJ
#        group3: CGK
#        group4: DHL
#
#
#       D           G              K
#    A CD         E GH            JKL
#    ABCD         EFGH           IJKL
#    ________________________________
#     V1           V2             V3
#              somexlabel
#
#    txt file:
#    V1 A B C D
#    V2 E F G H
#    V3 I J K L
#
    data_table = {}
    all_found_algs     = []
    all_found_learners = []
    all_found_msgsizes = []
    
    all_algs_dirs = [d for d in glob.glob("../*") if "broadcast" not in d]
    for algdir in all_algs_dirs :
        alg_name = re.split("/", algdir)[1]
        all_found_algs += [alg_name]
        pattern = algdir + "/" + getDirectoryPattern()
        alg_learners = sorted([ int(v) for v in getAllValsFromDirs(pattern, 3) ])
        alg_sizes    = sorted([ int(v) for v in getAllValsFromDirs(pattern, 9) ])
        
        for numLearners in alg_learners :
            for msgSize in alg_sizes :
                overall_dir = algdir + "/overall_" + getDirectoryPattern(alg_name, "all", numLearners, 1, 1, msgSize, False)
                maxtp_msgps = getfilelinecolum(overall_dir + "/tp_lat_max.log", 3, 2)
                maxtp_mbps  = getfilelinecolum(overall_dir + "/tp_lat_max.log", 3, 3)
                lat_avg     = getfilelinecolum(overall_dir + "/tp_lat_75.log" , 3, 4)
                lat_50      = getfilelinecolum(overall_dir + "/tp_lat_75.log" , 3, 5)
                lat_95      = getfilelinecolum(overall_dir + "/tp_lat_75.log" , 3, 6)
                data_table[(alg_name, numLearners, msgSize)] = (prettynames[alg_name], maxtp_msgps, maxtp_mbps, lat_avg, lat_50, lat_95)
        
        all_found_learners += [ l for l in alg_learners if l not in all_found_learners ]
        all_found_msgsizes += [ s for s in alg_sizes    if s not in all_found_msgsizes ]

    all_found_learners.sort()
    all_found_msgsizes.sort()

    for msgSize in all_found_msgsizes :
        bcast_filepath = "../broadcast_%s.log" % msgSize
        broadcast_file = open(bcast_filepath, "w")
        broadcast_file.write("# learners [name msg/s Mbps latency_avg latency_50th latency_95]*\n")
        for numLearners in [0] + all_found_learners :
            logLine = "%s" % numLearners
            for alg in all_found_algs :
                vals = (prettynames[alg], 0, 0, 0, 0, 0)
                if data_table.has_key((alg,numLearners,msgSize)) :
                    vals = data_table[(alg,numLearners,msgSize)]
                logLine += " \t%s \t%s \t%s \t%s \t%s \t%s" % vals
            logLine += "\n"
            broadcast_file.write(logLine)
        broadcast_file.close()
    
    return all_found_msgsizes
    
    
####################################################################################################
####################################################################################################



####################################################################################################
# main code
####################################################################################################
doOverallPlotting = False
if len(sys.argv) > 1 :
    doOverallPlotting   = sys.argv[1] in ["True","true","T","t","1"]

doBroadcastPlotting = False
if len(sys.argv) > 2 :
    doBroadcastPlotting = sys.argv[2] in ["True","true","T","t","1"]

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
                        lat_max_median = generate_max_and_75_tp_lat_files(allThroughputsLatencies, int(size), overall_dir_name, alg, learners, groups, pxpg, size, wdisk)
                        if doOverallPlotting :
                            plot_overalls(overall_dir_name, size, lat_max_median*1.5)

bcast_sizes = createBroadcastData()
if doBroadcastPlotting or True :
    plot_broadcast(bcast_sizes)
                        
