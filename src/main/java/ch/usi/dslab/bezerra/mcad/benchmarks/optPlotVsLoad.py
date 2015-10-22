#!/usr/bin/python

import glob, os, re, sys
from benchCommon import *

####################################################################################################
# definitions
####################################################################################################
opt_gnuplot_script_sh_path = HOME + "/libmcad/benchLink/plot_opt.sh"
simple_gnuplot_script_sh_path = HOME + "/libmcad/benchLink/plot_simple_tplat_cdf.sh"
broadcast_gnuplot_sh_path = HOME + "/libmcad/benchLink/plot_broadcast.sh"
bcast_cdfs_gnuplot_sh_path = HOME + "/libmcad/benchLink/plot_bcast_cdfs.sh"
bcast_multi_bcast_gnuplot_sh_path = HOME + "/libmcad/benchLink/plot_multi_broadcast.sh"
bcast_multi_cdfs_gnuplot_sh_path = HOME + "/libmcad/benchLink/plot_multi_cdfs.sh"
mcast_multi_mcast_gnuplot_sh_path = HOME + "/libmcad/benchLink/plot_multi_multicast.sh"
prettynamesbcast = {"libpaxos" : "LibPaxos",
                    "lpnorand" : "LibPaxos",
                    "ridge"    : "Ridge"   ,
                    "mrp"      : "\"Ring Paxos\"",
                    "spread"   : "Spread",
                    "cfabcast" : "CFABCast"}
prettynamesmcast = {"libpaxos" : "LibPaxos",
                    "lpnorand" : "LibPaxos",
                    "ridge"    : "Ridge"   ,
                    "mrp"      : "\"Multi-Ring Paxos\"",
                    "spread"   : "Spread",
                    "cfabcast" : "CFABCast"}

class DataUnit :
    #load
    load = None
    
    #throughput
    tp_msgps = None
    tp_mbps  = None
    
    #consLatency
    lat_avg = None
    lat_50  = None
    lat_95  = None
    lat_99  = None
    
    def __init__(self, load=None, tp_msgps=None, tp_mbps=None, lat_avg=None, lat_50=None, lat_95=None, lat_99=None):
        self.load = load
        self.tp_msgps = tp_msgps
        self.tp_mbps  = tp_mbps
        self.lat_avg = lat_avg
        self.lat_50  = lat_50
        self.lat_95  = lat_95
        self.lat_99  = lat_99
    
    def toString(self) :
        return "%s %s %s %s %s %s %s" % (self.load, self.tp_msgps, self.tp_mbps, \
                                         self.lat_avg, self.lat_50, self.lat_95, self.lat_99)
    def __str__(self):
        return self.toString()
    def __unicode__(self):
        return self.toString()
    def __repr__(self):
        return self.toString()

class DataSummary :
    alg_name = None
    numLearners = None
    numGroups = None
    msgSize = None
    prettyname = None
    data_max = None
    data_75 = None
    data_power = None
    data_1client = None
    
    def __init__(self, alg_name=None, numLearners=None, numGroups=None, msgSize=None, prettyname=None, data_max=None, data_75=None, data_power=None, data_1client=None):
        self.alg_name = alg_name
        self.numLearners = numLearners
        self.numGroups = numGroups
        self.msgSize = msgSize
        self.prettyname = prettyname
        self.data_max = data_max
        self.data_75 = data_75
        self.data_power = data_power
        self.data_1client = data_1client
        
    def getDataUnit(self, name):
        return {
            "max"     : self.data_max,
            "1client" : self.data_1client,
            "75"      : self.data_75,
            "power"   : self.data_power,
        }[name]
    
    def toString(self) :
        return "%s %s %s %s %s %s %s %s %s" % \
          (self.alg_name, self.numLearners, self.numGroups,  self.msgSize, self.prettyname,\
           self.data_max, self.data_75,     self.data_power, self.data_1client)
    
    def __str__(self):
        return self.toString()
    def __unicode__(self):
        return self.toString()
    def __repr__(self):
        return self.toString()
    
    @staticmethod
    def emptySummary(name) :
        emptyUnit = DataUnit(0, 0, 0, 0, 0, 0, 0)
        emptySum = DataSummary("xxx", 0, 0, 0, name, emptyUnit, emptyUnit, emptyUnit, emptyUnit)
        return emptySum

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

def getAvgConsLatency(d) :
    l = getfileline(d + "/latency_conservative_average.log", 3)
    return float(l.split()[2]) if l != None else None

def getAvgOptLatency(d) :
    l = getfileline(d + "/latency_optimistic_average.log", 3)
    return float(l.split()[2]) if l != None else None

def getMistakes(d) :
    l = getfileline(d + "/mistakes_server_average.log", 3)
    return 100.0 * float(l.split()[2]) if l != None else None

def getAggThroughput(d) :
    l = getfileline(d + "/throughput_conservative_aggregate.log", 3)
    return float(l.split()[2]) if l != None else None

def saveToFile(filepath, pointlist) :
    # p is a tuple (x,y)
    f = open(filepath, "w")
    for p in pointlist :
        f.write("%s %s\n" % p)
    f.close()

def saveToFile2(filepath, pointlist) :
    # p is a tuple (x,y,z)
    f = open(filepath, "w")
    for p in pointlist :
        f.write("%s %s %s\n" % p)
    f.close()

def plot_overall(dirPath, msgSize, lat_max_avg) :
    print "Plotting in directory %s" % dirPath
    os.system("%s %s %s %s" % (opt_gnuplot_script_sh_path, dirPath, msgSize, lat_max_avg))

def plot_broadcast_tp_lat(sizes) :
#     for size in sizes :
#         print "Plotting broadcast graphs for %s bytes" % (size)
#         os.system("%s %s %s" % (broadcast_gnuplot_sh_path, os.getcwd() + "/../", size))
    print "Plotting broadcast multi graphs"
    os.system("%s %s" % (bcast_multi_bcast_gnuplot_sh_path, os.getcwd() + "/../"))

def plot_multicast_tp_lat() :
    print "Plotting multicast multi graphs"
    os.system("%s %s" % (mcast_multi_mcast_gnuplot_sh_path, os.getcwd() + "/../"))

def plot_broadcast_cdfs(logdir, data_table, all_algs, all_learners, all_sizes) :
    for tptype in ["1client", "power"] :
        for learners in all_learners :
            cdf_paths = dict()
            max_lat = dict()
            for size in all_sizes :
                #print "Plotting broadcast cdf graphs for (%s learners, %s bytes, %s) " % (learners, size, tptype)
                gnuplot_params = ""
                cdf_paths[size] = dict()
                max_lat[size] = 0
                for alg in all_algs :
                    cdf_paths[size][alg]  = "%s/%s/overall_%s" % (logdir,alg,getDirectoryPattern(alg, "all", learners, 1, 1, size, False))
                    cdf_paths[size][alg] += "/cdf_%s.log" % (tptype)
                    lat99 = float(data_table[(alg,learners,size)].getDataUnit(tptype).lat_95)
                    if lat99 > max_lat[size] : max_lat[size] = lat99
                    
                    gnuplot_params += " " + prettynamesbcast[alg] + " " + cdf_paths[size][alg]
                #os.system("%s %s %s %s %s %s %s" % (bcast_cdfs_gnuplot_sh_path, size, learners, os.getcwd() + "/../", tptype, gnuplot_params, max_lat[size]))
            
            print "Plotting broadcast cdf multiplot for (%s learners, %s)" % (learners, tptype)
            multi_params = "%s %s " % (learners, os.getcwd() + "/../")
            for alg in all_algs :
                multi_params += "%s " % (prettynamesbcast[alg])
            rsizes = list(all_sizes)
            rsizes.reverse()
            for size in rsizes :
                for alg in all_algs :
                    multi_params += "%s " % (cdf_paths[size][alg])
            for size in rsizes :
                multi_params+= "%s " % (max_lat[size])
            multi_params += "%s" % (tptype)
            
            os.system("%s %s" % (bcast_multi_cdfs_gnuplot_sh_path, multi_params))
            
                
            # % learners, "..", (for alg in algs, insert here), 
            # learners=$1
            # outpath=$2
            # alg1=$3
            # alg2=$4
            # alg3=$5
            # alg4=$6
            # alg1path64k=$7
            # alg2path64k=$8
            # alg3path64k=$9
            # alg4path64k=${10}
            # alg1path8k=${11}
            # alg2path8k=${12}
            # alg3path8k=${13}
            # alg4path8k=${14}
            # alg1path200=${15}
            # alg2path200=${16}
            # alg3path200=${17}
            # alg4path200=${18}
            # maxlat64k=${19}
            # maxlat8k=${20}
            # maxlat200=${21}

            

def create_cdf_plot(line_cdf_file, plot_file_name) :
#     print "Createing cdf plot file for (%s,%s)" % (line_cdf_file, plot_file_name)
    lineCdf = getfileline(line_cdf_file, 3)
    lineCdfElements = lineCdf.split()
    properCdf = []
    totalCount = 0
    for i in range(1, len(lineCdfElements), 2) :
        x = int  (lineCdfElements[i  ])  # bucketRange
        y = float(lineCdfElements[i + 1])  # bucketCount
        properCdf.append((x, y))
        totalCount = y
    properCdfFile = open(plot_file_name, "w")
    latency_50th = None
    latency_95th = None
    latency_99th = None
    for point in properCdf :
        latency_bucket = point[0]
        percentile = point[1] / totalCount
        properCdfFile.write("%s %s\n" % (latency_bucket, percentile))
        if latency_50th == None and percentile >= 0.50 :
            latency_50th = latency_bucket
        if latency_95th == None and percentile >= 0.95 :
            latency_95th = latency_bucket
        if latency_99th == None and percentile >= 0.99 :
            latency_99th = latency_bucket
    properCdfFile.close()
    return (latency_50th, latency_95th, latency_99th)

def generate_max_and_75_and_power_tp_lat_files(allThroughputsLatencies, msgSize, overall_dir_name, alg, learners, groups, pxpg, sizeStr, wdisk) :
    load_max = tp_max = lat_max = optlat_max = 0
    power_max = load_power = tp_power = lat_power = optlat_power = 0
    load_1client = tp_1client = lat_1client = optlat_1client = None
    for point in allThroughputsLatencies :
        load, tp, lat, optlat, mistakes = point
        power = tp / lat
        if tp > tp_max :
            load_max = load
            tp_max = tp
            lat_max = lat
            optlat_max = optlat
        if power > power_max :
            power_max = power
            load_power = load
            tp_power = tp
            lat_power = lat
            optlat_power = optlat
        if load_1client == None :
            load_1client = load
            tp_1client = tp
            lat_1client = lat
            optlat_1client = optlat

    tp_75_ideal = 0.75 * tp_max
    load_75_estimate = int(round(0.75 * load_max))
    tp_75_found = load_75_found = lat_75_found = optlat_75_found = 0
    lowest_distance = float("inf")
    for point in allThroughputsLatencies :
        load, tp, lat, optlat, mistakes = point
        distance = abs(tp - tp_75_ideal)
        if distance < lowest_distance : 
            lowest_distance = distance
            load_75_found = load
            tp_75_found = tp
            lat_75_found = lat
            optlat_75_found = optlat

    # latency cdf load_tp_max
    load_max_directory = getDirectoryPattern(alg, load_max, learners, groups, pxpg, size, wdisk)
    cdf_cons_max_file = load_max_directory + "/latencydistribution_conservative_aggregate.log"
    cdf_opt__max_file = load_max_directory + "/latencydistribution_optimistic_aggregate.log"
    latency_cons_50th_max, latency_cons_95th_max, latency_cons_99th_max = create_cdf_plot(cdf_cons_max_file, overall_dir_name + "/cdf_max_cons.log")
    latency_opt__50th_max, latency_opt__95th_max, latency_opt__99th_max = create_cdf_plot(cdf_opt__max_file, overall_dir_name + "/cdf_max_opt.log")
    
    # latency cdf load_tp_75
    load_75_directory = getDirectoryPattern(alg, load_75_found, learners, groups, pxpg, size, wdisk)
    cdf_cons_75_file = load_75_directory + "/latencydistribution_conservative_aggregate.log"
    cdf_opt__75_file = load_75_directory + "/latencydistribution_optimistic_aggregate.log"
    latency_cons_50th_75, latency_cons_95th_75, latency_cons_99th_75 = create_cdf_plot(cdf_cons_75_file, overall_dir_name + "/cdf_75_cons.log")
    latency_opt__50th_75, latency_opt__95th_75, latency_opt__99th_75 = create_cdf_plot(cdf_opt__75_file, overall_dir_name + "/cdf_75_opt.log")
    
    # latency cdf load_power
    load_power_directory = getDirectoryPattern(alg, load_power, learners, groups, pxpg, size, wdisk)
    cdf_cons_power_file = load_power_directory + "/latencydistribution_conservative_aggregate.log"
    cdf_opt__power_file = load_power_directory + "/latencydistribution_optimistic_aggregate.log"
    latency_cons_50th_power, latency_cons_95th_power, latency_cons_99th_power = create_cdf_plot(cdf_cons_power_file, overall_dir_name + "/cdf_power_cons.log")
    latency_opt__50th_power, latency_opt__95th_power, latency_opt__99th_power = create_cdf_plot(cdf_opt__power_file, overall_dir_name + "/cdf_power_opt.log")

    # latency cdf 1 client
    load_1client_directory = getDirectoryPattern(alg, load_1client, learners, groups, pxpg, size, wdisk)
    cdf_cons_1client_file = load_1client_directory + "/latencydistribution_conservative_aggregate.log"
    cdf_opt__1client_file = load_1client_directory + "/latencydistribution_optimistic_aggregate.log"
    latency_cons_50th_1client, latency_cons_95th_1client, latency_cons_99th_1client = create_cdf_plot(cdf_cons_1client_file, overall_dir_name + "/cdf_1client_cons.log")
    latency_opt__50th_1client, latency_opt__95th_1client, latency_opt__99th_1client = create_cdf_plot(cdf_opt__1client_file, overall_dir_name + "/cdf_1client_opt.log")
        
    # max file
    file_max = open(overall_dir_name + "/tp_lat_max.log", "w")
    file_max.write("# maxtp = %s (load %s), ideal 0.75maxtp = %s (estimated load %s)\n" % (tp_max, load_max, tp_75_ideal, load_75_estimate))
    file_max.write("# load throughput(msg/s) throughput(MBps) latency_cons_avg(ms) latency_cons_50th(ms) latency_cons_95th(ms) latency_cons_99th(ms) latency_opt__avg(ms) latency_opt__50th(ms) latency_opt__95th(ms) latency_opt__99th(ms):\n")
    file_max.write("%s %s %s %s %s %s %s %s %s %s %s\n" % (load_max, tp_max, tp_max * msgSize * 8 / 1e6, lat_max / 1e6, latency_cons_50th_max / 1e6, latency_cons_95th_max / 1e6, latency_cons_99th_max / 1e6, optlat_max / 1e6, latency_opt__50th_max / 1e6, latency_opt__95th_max / 1e6, latency_opt__99th_max / 1e6 ))
    file_max.close()
    
    # 75 found file
    tp_distance_percent = 100 * abs(tp_75_found - tp_75_ideal) / tp_75_ideal
    load_distance_percent = 100 * abs(load_75_found - load_75_estimate) / load_75_estimate
    file_75 = open(overall_dir_name + "/tp_lat_75.log", "w")
    file_75.write("# maxtp = %s (load %s), ideal 0.75maxtp = %s (estimated load %s), found 0.75maxtp = %s (load %s): distance %s%% (load distance %s%%)\n" \
                  % (tp_max, load_max, tp_75_ideal, load_75_estimate, tp_75_found, load_75_found, int(round(tp_distance_percent)), int(round(load_distance_percent))))
    file_75.write("# load throughput(msg/s) throughput(MBps) latency_cons_avg(ms) latency_cons_50th(ms) latency_cons_95th(ms) latency_cons_99th(ms) latency_opt__avg(ms) latency_opt__50th(ms) latency_opt__95th(ms) latency_opt__99th(ms):\n")
    file_75.write("%s %s %s %s %s %s %s %s %s %s %s\n" % (load_75_found, tp_75_found, tp_75_found * msgSize * 8 / 1e6, lat_75_found / 1e6, latency_cons_50th_75 / 1e6, latency_cons_95th_75 / 1e6, latency_cons_99th_75 / 1e6, optlat_75_found / 1e6, latency_opt__50th_75 / 1e6, latency_opt__95th_75 / 1e6, latency_opt__99th_75 / 1e6))
    file_75.close()
    
    # power file
    file_power = open(overall_dir_name + "/tp_lat_power.log", "w")
    file_power.write("# load throughput(msg/s) throughput(MBps) latency_cons_avg(ms) latency_cons_50th(ms) latency_cons_95th(ms) latency_cons_99th(ms) latency_opt__avg(ms) latency_opt__50th(ms) latency_opt__95th(ms) latency_opt__99th(ms):\n#\n")
    file_power.write("%s %s %s %s %s %s %s %s %s %s %s\n" % (load_power, tp_power, tp_power * msgSize * 8 / 1e6, lat_power / 1e6, latency_cons_50th_power / 1e6, latency_cons_95th_power / 1e6, latency_cons_99th_power / 1e6, optlat_power / 1e6, latency_opt__50th_power / 1e6, latency_opt__95th_power / 1e6, latency_opt__99th_power / 1e6))
    file_power.close()
    
    # single client file
    file_1client = open(overall_dir_name + "/tp_lat_1client.log", "w")
    file_1client.write("# load throughput(msg/s) throughput(MBps) latency_cons_avg(ms) latency_cons_50th(ms) latency_cons_95th(ms) latency_cons_99th(ms) latency_opt__avg(ms) latency_opt__50th(ms) latency_opt__95th(ms) latency_opt__99th(ms):\n#\n")
    file_1client.write("%s %s %s %s %s %s %s %s %s %s %s\n" % (load_1client, tp_1client, tp_1client * msgSize * 8 / 1e6, lat_1client / 1e6, latency_cons_50th_1client / 1e6, latency_cons_95th_1client / 1e6, latency_cons_99th_1client / 1e6, optlat_1client / 1e6, latency_opt__50th_1client / 1e6, latency_opt__95th_1client / 1e6, latency_opt__99th_1client / 1e6))
    file_1client.close()
    
    # critical points file
    file_criticals = open(overall_dir_name + "/criticals.log", "w")
    file_criticals.write("#load_max tp_max load_75 tp_75 load_power tp_power load_1client tp_1_client\n")
    file_criticals.write("%s %s 0.025 %s %s 0.02 %s %s 0.015 %s %s 0.01\n" % (load_max, tp_max, load_75_found, tp_75_found, load_power, tp_power, load_1client, tp_1client))
    file_criticals.close()
    
    return latency_cons_50th_max

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
    all_found_algs = []
    all_found_learners = []
    all_found_msgsizes = []
    
    all_algs_dirs_glob = [d for d in glob.glob("../*") if "broadcast" not in d and ".p" not in d  and "multicast" not in d]
    all_algs_dirs = []
    for alg in ["spread", "lpnorand", "mrp", "ridge"] :
        algdir = "../" + alg
        if algdir in all_algs_dirs_glob :
            all_algs_dirs.append(algdir)
    
    for algdir in all_algs_dirs :
        alg_name = re.split("/", algdir)[1]
        all_found_algs += [alg_name]
        pattern = algdir + "/" + getDirectoryPattern()
        alg_learners = sorted([ int(v) for v in getAllValsFromDirs(pattern, 3) ])
        alg_sizes = sorted([ int(v) for v in getAllValsFromDirs(pattern, 9) ])
        
        for numLearners in alg_learners :
            for msgSize in alg_sizes :
                data_units = {}
                overall_dir = algdir + "/overall_" + getDirectoryPattern(alg_name, "all", numLearners, 1, 1, msgSize, False)
                for data_type in ["max","75","power","1client"] :
                    data_file = overall_dir + "/tp_lat_%s.log" % (data_type)
                    load     = getfilelinecolum(data_file, 3, 1)
                    tp_msgps = getfilelinecolum(data_file, 3, 2)
                    tp_mbps  = getfilelinecolum(data_file, 3, 3)
                    lat_avg  = getfilelinecolum(data_file, 3, 4)
                    lat_50   = getfilelinecolum(data_file, 3, 5)
                    lat_95   = getfilelinecolum(data_file, 3, 6)
                    lat_99   = getfilelinecolum(data_file, 3, 7)
                    data_units[data_type] = DataUnit(load, tp_msgps, tp_mbps, lat_avg, lat_50, lat_95, lat_99)
                data_table[(alg_name, numLearners, msgSize)] = \
                  DataSummary(alg_name, numLearners, 1, msgSize, prettynamesbcast[alg_name], \
                  data_units["max"], data_units["75"], data_units["power"], data_units["1client"])
        
        all_found_learners += [ l for l in alg_learners if l not in all_found_learners ]
        all_found_msgsizes += [ s for s in alg_sizes    if s not in all_found_msgsizes ]

    all_found_learners.sort()
    all_found_msgsizes.sort()

    for msgSize in all_found_msgsizes :
        bcast_filepath = "../broadcast_%s.log" % msgSize
        broadcast_file = open(bcast_filepath, "w")
        broadcast_file.write("# learners [name load msg/s Mbps latency_avg latency_50th latency_95 latency_99]x{max, 75, power, 1client}*\n")
        for numLearners in [0] + all_found_learners :
            logLine = "%s " % numLearners
            for alg in all_found_algs :
                vals = DataSummary.emptySummary(prettynamesbcast[alg])
                if data_table.has_key((alg, numLearners, msgSize)) :
                    vals = data_table[(alg, numLearners, msgSize)]
                logLine += vals.toString() + " "
            logLine += "\n"
            broadcast_file.write(logLine)
        broadcast_file.close()
    
    return (data_table, all_found_algs, all_found_learners, all_found_msgsizes)

def createMulticastData() :
    # assuming that the script is run inside logsmcast/algorithm/
    # create a table of (alg,groups,msgsize) -> (maxtpmsgps,maxtpmbps,lat_avg,lat_50,lat_95)
    
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
    all_found_algs = []
    all_found_groups = []
    all_found_msgsizes = []
    
    all_algs_dirs_glob = [d for d in glob.glob("../*") if "broadcast" not in d and ".p" not in d and "multicast" not in d]
    all_algs_dirs = []
    for alg in ["spread", "mrp", "ridge"] :
        algdir = "../" + alg
        if algdir in all_algs_dirs_glob :
            all_algs_dirs.append(algdir)
    
    for algdir in all_algs_dirs :
        alg_name = re.split("/", algdir)[1]
        all_found_algs += [alg_name]
        pattern = algdir + "/" + getDirectoryPattern()
        alg_groups = sorted([ int(v) for v in getAllValsFromDirs(pattern, 5) ])
        alg_sizes = sorted([ int(v) for v in getAllValsFromDirs(pattern, 9) ])
        
        for numGroups in alg_groups :
            for msgSize in alg_sizes :
                data_units = {}
                overall_dir = algdir + "/overall_" + getDirectoryPattern(alg_name, "all", numGroups*4, numGroups, 1, msgSize, False)
                for data_type in ["max","75","power","1client"] :
                    data_file = overall_dir + "/tp_lat_%s.log" % (data_type)
                    load     = getfilelinecolum(data_file, 3, 1)
                    tp_msgps = getfilelinecolum(data_file, 3, 2)
                    tp_mbps  = getfilelinecolum(data_file, 3, 3)
                    lat_avg  = getfilelinecolum(data_file, 3, 4)
                    lat_50   = getfilelinecolum(data_file, 3, 5)
                    lat_95   = getfilelinecolum(data_file, 3, 6)
                    lat_99   = getfilelinecolum(data_file, 3, 7)
                    data_units[data_type] = DataUnit(load, tp_msgps, tp_mbps, lat_avg, lat_50, lat_95, lat_99)
                data_table[(alg_name, numGroups, msgSize)] = \
                  DataSummary(alg_name, numGroups*4, numGroups, msgSize, prettynamesmcast[alg_name], \
                  data_units["max"], data_units["75"], data_units["power"], data_units["1client"])
        
        all_found_groups += [ g for g in alg_groups if g not in all_found_groups ]
        all_found_msgsizes += [ s for s in alg_sizes    if s not in all_found_msgsizes ]

    all_found_groups.sort()
    all_found_msgsizes.sort()

    for msgSize in all_found_msgsizes :
        mcast_filepath = "../multicast_%s.log" % msgSize
        multicast_file = open(mcast_filepath, "w")
        multicast_file.write("# groups [name load msg/s Mbps latency_avg latency_50th latency_95 latency_99]x{max, 75, power, 1client}*\n")
        for numGroups in [0] + all_found_groups :
            logLine = "%s " % numGroups
            for alg in all_found_algs :
                vals = DataSummary.emptySummary(prettynamesmcast[alg])
                if data_table.has_key((alg, numGroups, msgSize)) :
                    vals = data_table[(alg, numGroups, msgSize)]
                logLine += vals.toString() + " "
            logLine += "\n"
            multicast_file.write(logLine)
        multicast_file.close()
    
    return (data_table, all_found_algs, all_found_groups, all_found_msgsizes)
####################################################################################################
####################################################################################################



####################################################################################################
# main code
####################################################################################################
doOverallPlotting = False
if len(sys.argv) > 1 :
    doOverallPlotting = sys.argv[1] in ["True", "true", "T", "t", "1"]



# libpaxos_10_clients_16_learners_1_groups_1_pxpergroup_140_bytes_diskwrite_False
alldirs = "*_clients_*learners*groups*pxpergroup*"

all_algs = getAllValsFromDirs(alldirs, 0)
all_clis = getAllValsFromDirs(alldirs, 1)
all_learners = getAllValsFromDirs(alldirs, 3)
all_groups = getAllValsFromDirs(alldirs, 5)
all_pxpg = getAllValsFromDirs(alldirs, 7)
all_sizes = getAllValsFromDirs(alldirs, 9)
all_disks = getAllValsFromDirs(alldirs, 12)

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
                        allConservativeLatencies = []
                        allOptimisticLatencies = []
                        allMistakes = []
                        allThroughputs = []
                        allTpMistakes = []
                        allPowers = []
                        allThroughputsLatencies = []
                        for cli in clis :
                            cliDir = getDirectoryPattern(alg, cli, learners, groups, pxpg, size, wdisk)
                            consLatency = getAvgConsLatency(cliDir)
                            optLatency = getAvgOptLatency(cliDir)
                            mistakes = getMistakes(cliDir)
                            throughput = getAggThroughput(cliDir)
                            if consLatency == None or throughput == None or optLatency == None:
                                continue
                            allConservativeLatencies.append((cli, consLatency))
                            allOptimisticLatencies  .append((cli, optLatency))
                            allMistakes             .append((cli, mistakes))
                            allThroughputs.append((cli, throughput))
                            allTpMistakes .append((cli, throughput, mistakes))
                            allPowers     .append((cli, throughput / consLatency))
                            allThroughputsLatencies.append((cli, throughput, consLatency, optLatency, mistakes))
                        if not allConservativeLatencies or not allThroughputs or not allOptimisticLatencies:
                            print "No valid points for %s. Skipping." % (overall_dir_name)
                            continue

                        overall_consLatency_file = overall_dir_name + "/consLatency.log"
                        overall_optLatency_file = overall_dir_name + "/optLatency.log"
                        overall_mistakes_file = overall_dir_name + "/mistakes.log"
                        overall_throughput_file = overall_dir_name + "/throughput.log"
                        overall_tpmistakes_file = overall_dir_name + "/tp_mistakes.log"
                        overall_power_file = overall_dir_name + "/power.log"
                        overall_max_tp_file = overall_dir_name + "/tp_lat_max.log"
                        overall_75_tp_file = overall_dir_name + "/tp_lat_75.log"
                        if not os.path.exists(overall_dir_name) :
                            os.makedirs(overall_dir_name)
                        saveToFile(overall_consLatency_file, allConservativeLatencies)
                        saveToFile(overall_optLatency_file, allOptimisticLatencies)
                        saveToFile(overall_mistakes_file, allMistakes)
                        saveToFile(overall_throughput_file, allThroughputs)
                        saveToFile2(overall_tpmistakes_file, allTpMistakes)
                        saveToFile(overall_power_file, allPowers)
                        lat_max_median = generate_max_and_75_and_power_tp_lat_files(allThroughputsLatencies, int(size), overall_dir_name, alg, learners, groups, pxpg, size, wdisk)
                        if doOverallPlotting :
                            plot_overall(overall_dir_name, size, lat_max_median * 1.5)
