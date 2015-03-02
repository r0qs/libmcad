#!/usr/bin/python

import sys

BUCKET_RANGE_NANO = 10000 # us

def convertLogs(path) :
    with open(path + "/client_tp_lat.csv", "r") as tplatfile:
        latOneLines = []
        tpOneLines = []
        bucketSet = {}
        latCDF = {}
        
        aggregatedLatency = 0.0
        numItems = 0.0
        msgSize = 0
        duration = 0
        lastTimestamp = None
        timestampDeliveries = 0
        
        for line in tplatfile:
            numItems += 1.0
            vals = eval(line)
            numOutstandings = vals[0]
            msgSize         = vals[1]
            timestamp       = vals[2]
            latency_ms      = vals[3]

            duration      = timestamp if timestamp > duration else duration
            fakeTimestamp = 1424000000000 + 1000 * timestamp
            
            # latency
            latency_ns    = latency_ms * 1e3
            latOneLines.append("%s %s\n" % (fakeTimestamp, latency_ns))
            aggregatedLatency += latency_ns
            
            # latency distribution
            bucketRange = int(latency_ns / BUCKET_RANGE_NANO)
            if not bucketSet.has_key(bucketRange) :
                bucketSet[bucketRange] = 0
            bucketSet[bucketRange] += 1
            
            # throughput
            if lastTimestamp != None and timestamp != lastTimestamp :
                tpOneLines.append("%s %s %s\n" % (fakeTimestamp, 1000, timestampDeliveries))
                timestampDeliveries = 0
            lastTimestamp = timestamp
            timestampDeliveries += 1
            
        averageLatency = aggregatedLatency / numItems
        averageThroughput = numItems / duration
        
        latOneFile = open(path + "/latency_conservative_0.log", "w")
        latOneFile.write("# latency conservative 0\n")
        latOneFile.write("# latency (ns)\n")
        latOneFile.write("# average: 0 %s\n" % (averageLatency))
        for line in latOneLines :
            latOneFile.write(line)
        latOneFile.close()
        
        latAvgFile = open(path + "/latency_conservative_average.log", "w")
        latAvgFile.write("# latency conservative\n")
        latAvgFile.write("# latency (ns)\n")
        latAvgFile.write("0 %s\n" % (averageLatency))
        latAvgFile.close()
        
        sortedBucketRanges = sorted(bucketSet.keys())
        currentAggregate = 0
        for r in sortedBucketRanges :
            currentAggregate += bucketSet[r]
            latCDF[r] = currentAggregate
        latDistAverage   = "0 "
        latDistAggregate = "0 "
        for r in sortedBucketRanges :
            latDistAverage   += "%s %s " % (r, bucketSet[r])
            latDistAggregate += "%s %s " % (r, latCDF[r])
        latDistAverage   += "\n"
        latDistAggregate += "\n"
        latDistFile = open(path + "/latencydistribution_conservative_average.log", "w")
        latDistFile.write("# latencydistribution conservative\n")
        latDistFile.write("# latencydistribution (10000 ns buckets)\n")
        latDistFile.write(latDistAverage)
        latDistFile.close()
        latAggFile = open(path + "/latencydistribution_conservative_aggregate.log", "w")
        latAggFile.write("# latencydistribution conservative\n")
        latAggFile.write("# latencydistribution (10000 ns buckets)\n")
        latAggFile.write(latDistAggregate)
        latAggFile.close()
        
        tpOneFile = open(path + "/throughput_conservative_0.log", "w")
        tpOneFile.write("# throughput conservative 0\n")
        tpOneFile.write("# throughput (cps)\n")
        tpOneFile.write("# average: 0 0 %s\n" % (averageThroughput))
        for line in tpOneLines :
            tpOneFile.write(line)
        tpOneFile.close()
        
        tpAggFile = open(path + "/throughput_conservative_aggregate.log", "w")
        tpAggFile.write("# throughput conservative\n")
        tpAggFile.write("# throughput (cps)\n")
        tpAggFile.write("0 0 %s\n" % (averageThroughput))
        tpAggFile.close()

# in case this script receives a parameter (a path), run the function for that path
if len(sys.argv) == 1 :
    print "file %s can be either imported or run with one parameter (path of libpaxos experiment)" % (sys.argv[0])
else :
    path = sys.argv[1]
    convertLogs(path)
