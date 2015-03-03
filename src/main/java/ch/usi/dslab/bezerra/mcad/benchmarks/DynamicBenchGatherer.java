package ch.usi.dslab.bezerra.mcad.benchmarks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ch.usi.dslab.bezerra.mcad.benchmarks.BenchmarkEventList.EventInfo;
import ch.usi.dslab.bezerra.mcad.benchmarks.BenchmarkEventList.GlobalPermitEvent;
import ch.usi.dslab.bezerra.mcad.benchmarks.BenchmarkEventList.PermitEvent;
import ch.usi.dslab.bezerra.netwrapper.codecs.Codec;
import ch.usi.dslab.bezerra.netwrapper.codecs.CodecGzip;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPMessage;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPReceiver;

public class DynamicBenchGatherer {
   int expectedLogs;
   TCPReceiver receiver;
   List<BenchmarkEventList> allLists;
   BenchmarkEventList merged;
   String logDirectory;
   
   public DynamicBenchGatherer(int numClients, int port, String logDir) {
      expectedLogs = numClients;
      receiver = new TCPReceiver(port);
      logDirectory = logDir;
      allLists = new ArrayList<BenchmarkEventList>();
   }
   
   public void receive() {
      Codec gzip = new CodecGzip();
      while (expectedLogs > 0) {
         TCPMessage tcpmsg = receiver.receive();
         byte[] bytes = (byte[]) tcpmsg.getContents().getItem(0);
         BenchmarkEventList bel = (BenchmarkEventList) (gzip.createObjectFromBytes(bytes));
         allLists.add(bel);
         expectedLogs--;
      }
   }
   
   public void merge() {
      BenchmarkEventList aggregated = new BenchmarkEventList();
      merged = new BenchmarkEventList();
      int globalNumPermits = 0;
      for (BenchmarkEventList individualList : allLists)
         aggregated.Merge(individualList);
      while(aggregated.isEmpty() == false) {
         EventInfo ev = aggregated.takeNextEvent();
         if (ev.getType() == EventInfo.PERMIT_EVENT) {
            PermitEvent pev = (PermitEvent) ev;
            globalNumPermits += pev.newNumberOfPermits;
            GlobalPermitEvent gpev = new GlobalPermitEvent(pev.timestamp, pev.clientId, pev.newNumberOfPermits, globalNumPermits);
            merged.addEvent(gpev);
         }
         else {
            merged.addEvent(ev);            
         }
      }
   }
   
   public void saveMergedEventLog() {
      try {
         BenchmarkEventList toSave = new BenchmarkEventList(merged);
         Path mergedPath = Paths.get(logDirectory, "mergedDynamicLoad.log");
         BufferedWriter writer = Files.newBufferedWriter(mergedPath, StandardCharsets.UTF_8);
         while (toSave.isEmpty() == false) {
            EventInfo ev = toSave.takeNextEvent();
            writer.write(ev.toString() + "\n");
         }
         writer.close();
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }
   
   public void saveThroughputPlot() {
      final long INTERVAL = 100000L; // nanoseconds
      try {
         BenchmarkEventList toPlot = new BenchmarkEventList(merged);
         Path dataPath = Paths.get(logDirectory, "dynamicThroughputData.log");
         BufferedWriter writer = Files.newBufferedWriter(dataPath, StandardCharsets.UTF_8);
         String label = "";
         long lastTS = 0l;
         double currentIntervalDeliveries = 0d;
         while (toPlot.isEmpty() == false) {
            EventInfo ev = toPlot.takeNextEvent();
            if (lastTS == 0l) lastTS = ev.getTimestamp();
            if (ev.getType() == EventInfo.GLOBAL_PERMIT_EVENT) {
               GlobalPermitEvent gpev = (GlobalPermitEvent) ev;
               label = String.format(" %d", gpev.allPermits);
            }
            else {
               // update throughput
               currentIntervalDeliveries += 1;
               if (ev.getTimestamp() - lastTS > INTERVAL) {
                  double throughput = currentIntervalDeliveries / (ev.getTimestamp() - lastTS);
                  String fileLine = String.format("%d %d %s\n", ev.getTimestamp(), throughput, label);
                  writer.write(fileLine);
                  label = "";
                  currentIntervalDeliveries = 0;
                  lastTS = ev.getTimestamp();
               }
            }
         }
      }
      catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }
   
   public static void main(String[] args) {
      
      int numDynamicClients = Integer.parseInt(args[0]);
      int port = Integer.parseInt(args[1]);
      String directory = args[2];
      DynamicBenchGatherer dbg = new DynamicBenchGatherer(numDynamicClients, port, directory);
      
      dbg.receive();
      dbg.merge();
      dbg.saveMergedEventLog();
      dbg.saveThroughputPlot();
   }

}
