#!/usr/bin/python

#===latency file===#:
# latency conservative 3
# latency (ns)
# average: 0 1841031.305085]

def convertLogs(path) :
    with open(path = "/client_tp_lat.csv", "r") as tplatfile:
        latOneLines = []
        
        aggregatedLatency = 0.0
        numItems = 0.0
        msgSize = 0
        duration = 0
        
        for line in tplatfile:
            numItems += 1.0
            vals = eval(line)
            numOutstandings = vals[0]
            msgSize         = vals[1]
            timestamp       = vals[2]
            latency_ms      = vals[3]
            
            duration      = timestamp if timestamp > duration else duration
            fakeTimestamp = 1424873060000 + 1000 * timestamp
            latency_ns    = latency_ms * 1e6
            
            latOneLines.append("% %s\n", fakeTimestamp, latency_ms)
            
            aggregatedLatency += latency_ns
            
        averageLatency = aggregatedLatency / numItems
        
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
        
        tpAggFile = open(path + "/throughput_conservative_aggregate.log", "w")
        tpAggFile.write("# throughput conservative\n")
        tpAggFile.write("# throughput (cps)\n")
        tpAggFile.write("0 0 %s" % (numItems / duration))
        tpAggFile.close()