package ch.usi.dslab.bezerra.mcad.benchmarks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ch.usi.dslab.bezerra.mcad.benchmarks.BenchmarkEventList.EventInfo;
import ch.usi.dslab.bezerra.mcad.benchmarks.BenchmarkEventList.GlobalPermitEvent;
import ch.usi.dslab.bezerra.mcad.benchmarks.BenchmarkEventList.PermitEvent;
import ch.usi.dslab.bezerra.mcad.benchmarks.BenchmarkEventList.MessageCountEvent;
import ch.usi.dslab.bezerra.netwrapper.codecs.Codec;
import ch.usi.dslab.bezerra.netwrapper.codecs.CodecGzip;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPMessage;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPReceiver;

public class DynamicBenchGatherer {
   
   public static class CliPermits {
      Map<Integer, AtomicInteger> cliPermits = new HashMap<Integer, AtomicInteger>();
      public void setPermits(int cid, int num) {
         AtomicInteger cliPerms = cliPermits.get(cid);
         if (cliPerms == null) {
            cliPerms = new AtomicInteger(0);
            cliPermits.put (cid, cliPerms);
         }
         cliPerms.set(num);
      }
      public int getGlobalPermits() {
         int total = 0;
         for (AtomicInteger cp : cliPermits.values()) {
            total += cp.get();
         }
         return total;
      }
   }
   
   int expectedLogs;
   int port;
   TCPReceiver receiver;
   List<BenchmarkEventList> allLists;
   BenchmarkEventList merged;
   String logDirectory;
   
   public DynamicBenchGatherer(int numClients, int port, String logDir) {
      expectedLogs = numClients;
      this.port = port;
      logDirectory = logDir;
      allLists = new ArrayList<BenchmarkEventList>();
   }
   
   public void receive() {
      Codec gzip = new CodecGzip();
      receiver = new TCPReceiver(port);
      while (expectedLogs > 0) {
         TCPMessage tcpmsg = receiver.receive();
         Object v0 = tcpmsg.getContents().getItem(0);
         if (v0 instanceof Boolean) continue;
         byte[] bytes = (byte[]) v0;
         BenchmarkEventList bel = (BenchmarkEventList) (gzip.createObjectFromBytes(bytes));
         allLists.add(bel);
         expectedLogs--;
      }
      receiver.stop();
   }
   
   public void merge() {
      CliPermits cp = new CliPermits();
      BenchmarkEventList aggregated = new BenchmarkEventList();
      merged = new BenchmarkEventList();
      for (BenchmarkEventList individualList : allLists)
         aggregated.Merge(individualList);
      while(aggregated.isEmpty() == false) {
         EventInfo ev = aggregated.takeNextEvent();
         if (ev.getType() == EventInfo.PERMIT_EVENT) {
            PermitEvent pev = (PermitEvent) ev;
            int id = pev.clientId;
            cp.setPermits(id, pev.newNumberOfPermits);
            GlobalPermitEvent gpev = new GlobalPermitEvent(pev.timestamp, pev.clientId, pev.newNumberOfPermits, cp.getGlobalPermits());
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
      final long INTERVAL_MS = 1000; // milliseconds
      long start = 0;
      try {
         BenchmarkEventList toPlot = new BenchmarkEventList(merged);
         Path dataPath = Paths.get(logDirectory, "dynamicThroughputData.log");
         BufferedWriter writer = Files.newBufferedWriter(dataPath, StandardCharsets.UTF_8);
         String label = "";
         long lastTS = 0;
         double currentIntervalDeliveries = 0;
         while (toPlot.isEmpty() == false) {
            EventInfo ev = toPlot.takeNextEvent();
            if (lastTS == 0l) {
               lastTS = ev.getTimestamp();
               start = ev.getTimestamp();
            }
            if (ev.getType() == EventInfo.GLOBAL_PERMIT_EVENT) {
               GlobalPermitEvent gpev = (GlobalPermitEvent) ev;
               label = String.format("%d", gpev.allPermits);
            }
            else {
               MessageCountEvent mcev = (MessageCountEvent) ev;
               currentIntervalDeliveries += mcev.getMessageCount();
               if (ev.getTimestamp() > lastTS + INTERVAL_MS) {
                  double throughput = currentIntervalDeliveries / (ev.getTimestamp() - lastTS);
                  String fileLine = String.format("%f %f \"%s\"\n", (ev.getTimestamp() - start)/(1000d), throughput, label);
                  writer.write(fileLine);
                  label = "";
                  currentIntervalDeliveries = 0;
                  lastTS = ev.getTimestamp();
               }
            }
         }
         writer.close();
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
      System.out.println("DynamicBenchGatherer received all logs. Merging...");
      dbg.merge();
      System.out.println("DynamicBenchGatherer merged all logs. Saving to file...");
      dbg.saveMergedEventLog();
      System.out.println("DynamicBenchGatherer saved merged log to file. Creating throughput file...");
      dbg.saveThroughputPlot();
      System.out.println("DynamicBenchGatherer done.");
   }

}
