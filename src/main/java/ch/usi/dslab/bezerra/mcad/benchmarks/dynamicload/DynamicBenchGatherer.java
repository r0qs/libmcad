/*

 Libmcad - A multicast adaptor library
 Copyright (C) 2015, University of Lugano
 
 This file is part of Libmcad.
 
 Libmcad is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libmcad is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 
*/

/**
 * @author Eduardo Bezerra - eduardo.bezerra@usi.ch
 */

package ch.usi.dslab.bezerra.mcad.benchmarks.dynamicload;

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

import ch.usi.dslab.bezerra.mcad.benchmarks.dynamicload.BenchmarkEventList.EventInfo;
import ch.usi.dslab.bezerra.mcad.benchmarks.dynamicload.BenchmarkEventList.GlobalPermitEvent;
import ch.usi.dslab.bezerra.mcad.benchmarks.dynamicload.BenchmarkEventList.MessageCountEvent;
import ch.usi.dslab.bezerra.mcad.benchmarks.dynamicload.BenchmarkEventList.PermitEvent;
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
   
   public void savePlotFiles() {
      long start = 0;
      double lastThroughput = 0;
      try {
         BenchmarkEventList toPlot = new BenchmarkEventList(merged);
         Path throughputPath = Paths.get(logDirectory, "dynamicThroughputData.log");
         Path loadPath       = Paths.get(logDirectory, "dynamicLoadData.log");
         BufferedWriter   tpWriter = Files.newBufferedWriter(throughputPath, StandardCharsets.UTF_8);
         BufferedWriter loadWriter = Files.newBufferedWriter(loadPath,       StandardCharsets.UTF_8);
         while (toPlot.isEmpty() == false) {
            EventInfo ev = toPlot.takeNextEvent();
            if (start == 0) start = ev.getTimestamp();
            long relativeTimestamp = ev.getTimestamp() - start;
            if (ev.getType() == EventInfo.GLOBAL_PERMIT_EVENT) {
               GlobalPermitEvent gpev = (GlobalPermitEvent) ev;
               loadWriter.write(String.format("%d %f %d\n", relativeTimestamp, lastThroughput, gpev.allPermits));
            }
            else {
               MessageCountEvent mcev = (MessageCountEvent) ev;
               String throughputLine = String.format("%d %d %f %f\n", relativeTimestamp, mcev.getInterval(), mcev.getThroughput(), mcev.getAverageLatency());
               lastThroughput = mcev.getThroughput();
               tpWriter.write(throughputLine);
            }
         }
         tpWriter.close();
         loadWriter.close();
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
      System.out.println("DynamicBenchGatherer saved merged log to file. Creating plot files...");
      dbg.savePlotFiles();
      System.out.println("DynamicBenchGatherer done.");
   }

}
