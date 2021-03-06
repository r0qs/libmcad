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

package ch.usi.dslab.bezerra.mcad.tests;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.FastMulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.mcad.MulticastServer;
import ch.usi.dslab.bezerra.mcad.OptimisticMulticastAgent;
import ch.usi.dslab.bezerra.mcad.ridge.RidgeMulticastAgent;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.Merger;

public class TestServer {
   
   public static class StatusPrinter extends Thread {
      Map<Object, String> pendingMessages = new HashMap<Object, String>();
      private final int INTERVAL = 2500;
      private static StatusPrinter instance = null;
      boolean running = false;
      
      public static StatusPrinter getInstance() {
         if (instance == null) instance = new StatusPrinter();
         return instance;
      }
      
      private StatusPrinter() {
         super("StatusPrinter");
      }
      
      synchronized public void print(Object o, String text) {
         synchronized (pendingMessages) {
            pendingMessages.put(o, text);
         }
         if (running == false) {
            start();
            running = true;
         }
      }
      
      public void run() {
         while (true) {
            try {
               Thread.sleep(INTERVAL);
            } catch (InterruptedException e) {
               e.printStackTrace();
               System.exit(1);
            }
            
            synchronized (pendingMessages) {
               Collection<String> messages = pendingMessages.values();
               for (String msg : messages)
                  System.out.println(msg);
            }
         }
      }
   }

   public static class FastDelInversionCollector extends Thread {
      StatusPrinter printer;
      public FastDelInversionCollector (StatusPrinter printer) {
         super("FastDelInversionCollector");
         this.printer = printer;
      }
      public void run () {
         while (true) {
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
               e.printStackTrace();
               System.exit(1);
            }
            double inversionRate = Merger.getInversionRate();
            String invStatus = String.format("fast inversion rate = %.2f%%", inversionRate * 100.0d);
            printer.print(this, invStatus);
         }
      }
   }
   
   public static class LatencyCalculator extends Thread {
      private StatusPrinter printer;
      private DescriptiveStatistics consStats = new DescriptiveStatistics(10000);
      private DescriptiveStatistics  optStats = new DescriptiveStatistics(10000);
      private DescriptiveStatistics fastStats = new DescriptiveStatistics(10000);
      
      public LatencyCalculator(StatusPrinter printer) {
         super("LatencyCalculator");
         this.printer = printer;
      }
      
      public void addConsLatency(long value) {
         consStats.addValue(value);
      }
      
      public void addOptLatency(long value) {
         optStats.addValue(value);
      }
      
      public void addFastLatency(long value) {
         fastStats.addValue(value);
      }
      
      public void run () {
         while (true) {
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
               e.printStackTrace();
               System.exit(1);
            }
            
            String latencyLine = String.format(
                  "lat c: [%.1f - %.1f - %.1f] avg: %.2f last: %.2f\n" +
                  "    o: [%.1f - %.1f - %.1f] avg: %.2f last: %.2f\n" +
                  "    f: [%.1f - %.1f - %.1f] avg: %.2f last: %.2f",
                  consStats.getPercentile(25), consStats.getPercentile(50), consStats.getPercentile(75), consStats.getMean(), ((consStats.getN() > 0) ? consStats.getElement((int)(consStats.getN() - 1)) : Double.NaN),
                   optStats.getPercentile(25),  optStats.getPercentile(50),  optStats.getPercentile(75),  optStats.getMean(), (( optStats.getN() > 0) ?  optStats.getElement((int)( optStats.getN() - 1)) : Double.NaN),
                  fastStats.getPercentile(25), fastStats.getPercentile(50), fastStats.getPercentile(75), fastStats.getMean(), ((fastStats.getN() > 0) ? fastStats.getElement((int)(fastStats.getN() - 1)) : Double.NaN));
            
            printer.print(this, latencyLine);
         }
      }
   }
   
   public static class SpeculativeDeliveryVerifier extends Thread {
      public List<Long> speculativeSequence  = new LinkedList<Long>();
      public List<Long> conservativeSequence = new LinkedList<Long>();
      private StatusPrinter printer;
      private String name;
      private AtomicLong numDeliveries = new AtomicLong(0);
      private AtomicLong mistakes = new AtomicLong(0);
      private DescriptiveStatistics recentMistakes   = new DescriptiveStatistics(100000);
      
      public SpeculativeDeliveryVerifier(StatusPrinter printer, String name) {
         super("SpeculativeDeliveryVerifier");
         this.printer = printer;
         this.name    = name;
      }
      
      synchronized public void addSpeculativeDelivery(long mseq) {
         speculativeSequence.add(mseq);
         checkMatch();
      }
      
      synchronized public void addConservativeDelivery(long mseq) {
         conservativeSequence.add(mseq);
         numDeliveries.incrementAndGet();
         checkMatch();
      }
      
      synchronized private void checkMatch() {
         while (speculativeSequence.isEmpty() == false && conservativeSequence.isEmpty() == false) {
            long smid = speculativeSequence.remove(0);
            long cmid = conservativeSequence.remove(0);
            if (smid != cmid) {
               mistakes.incrementAndGet();
               recentMistakes.addValue(1);
            }
            else {
               recentMistakes.addValue(0);
            }
         }
      }
      
      public void run() {
         while (true) {
            
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
               e.printStackTrace();
               System.exit(1);
            }
            
            long m = mistakes.get();
            long d = numDeliveries.get();
            double rate = 100.0d * ((double) m) / ((double) d);
            double recentrate = 100.0d * (recentMistakes.getSum()) / ((double) recentMistakes.getN()); 
            String status = String.format("mistakes(%s): %d/%d = %.2f%%; rec: %.2f%%", name, m, d, rate, recentrate);
            printer.print(this, status);
         }
      }
   }
   
   public static class ListHashCalculator extends Thread {
      private String listName;
      private List<Object> objList;
      private String lastHash = "";
      private StatusPrinter printer;
      
      @SuppressWarnings({ "unchecked", "rawtypes" })
      public ListHashCalculator(StatusPrinter printer, String listName, List objList) {
         super ("ListHashCalculator");
         this.listName = listName;
         this.objList  = objList;
         this.printer  = printer;
      }
      
      public String byteArrayToHexString(byte[] b) {
         String result = "";
         for (int i=0; i < b.length; i++) {
           result +=
                 Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
         }
         return result;
       }
      
      public String toHash(byte[] hashMe) {
         MessageDigest md = null;
         try {
             md = MessageDigest.getInstance("SHA-1");
         }
         catch(NoSuchAlgorithmException e) {
             e.printStackTrace();
         } 
         return byteArrayToHexString(md.digest(hashMe));
      }
      
      boolean compactList() {
         synchronized(objList) {
            final int trimSize = 100;
            if (objList.size() >= trimSize) {
               String sequence = lastHash;
               for (int i = 0 ; i < trimSize ; i++) {
                  sequence += String.format(" %s", objList.get(0));
                  objList.remove(0);
               }
               lastHash = toHash(sequence.getBytes());
               return false;
            }
            else
               return true;
         }
      }
      
      String getListHash() {
         String sequence, hash;
         synchronized (objList) {
            while(compactList() == false);
            sequence = lastHash;
            for (Object obj : objList)
               sequence += String.format(" %s", obj);
            hash = toHash(sequence.getBytes()).substring(0, 7);
         }
         return hash;
      }
      
      public void run() {
         long printInterval = 2500;
         while (true) {
            try {
               Thread.sleep(printInterval);
            } catch (InterruptedException e) {
               e.printStackTrace();
               System.exit(1);
            }
            printer.print(this, (String.format("hash of [%s]: %s", listName, getListHash())));
         }
      }
   }
   
   public static class ConservativeDeliverer extends Thread {
      MulticastServer mcServer;
      List<Long> allDeliveries = new ArrayList<Long>();
      ListHashCalculator         allDeliveriesHashPrinter = new ListHashCalculator(StatusPrinter.getInstance(), "all", allDeliveries);
      Map<String, List<Long>> receivedMessages = new ConcurrentHashMap<String, List<Long>>();
      Map<String, ListHashCalculator> printers = new ConcurrentHashMap<String, ListHashCalculator>();
      SpeculativeDeliveryVerifier optVerifier;
      SpeculativeDeliveryVerifier fastVerifier;
      LatencyCalculator latencyCalculator;
      
      public ConservativeDeliverer(TestServer parent, SpeculativeDeliveryVerifier optVerifier, SpeculativeDeliveryVerifier fastVerifier, LatencyCalculator latencyCalculator) {
         super("ConservativeDeliverer");
         mcServer = parent.mcserver;
         this.optVerifier  = optVerifier;
         this.fastVerifier = fastVerifier;
         this.latencyCalculator = latencyCalculator;
         allDeliveriesHashPrinter.start();
      }
      
      void addDelivery(String destsStr, long mseq) {
         List<Long> destMsgs = receivedMessages.get(destsStr);
         if (destMsgs == null) {
            destMsgs = new ArrayList<Long>();
            receivedMessages.put(destsStr, destMsgs);
            ListHashCalculator lhp = new ListHashCalculator(StatusPrinter.getInstance(), destsStr, destMsgs);
            lhp.start();
         }
         synchronized (destMsgs) {
            destMsgs.add(mseq);
         }
         synchronized (allDeliveries) {
            allDeliveries.add(mseq);
         }
      }
      
      public void run() {
         while (true) {
            ClientMessage msg = mcServer.deliverClientMessage();
            long now = System.currentTimeMillis();
            
            int  clientId  = msg.getSourceClientId();
            long seq       = msg.getMessageSequence();
            long timestamp = (Long)   msg.getNext();
            String destStr = (String) msg.getNext();
            
            System.out.println(String.format("Delivered message %d.%d", clientId, seq));
            
            addDelivery(destStr, seq);
            
            Message reply = new Message(seq, DeliveryType.CONS);
            mcServer.sendReply(clientId, reply);

            long latency = now - timestamp;  
            latencyCalculator.addConsLatency(latency);
//            System.out.println(String.format("cons-delivered message %s within %d ms", mid, now - timestamp));
            optVerifier.addConservativeDelivery(seq);
            fastVerifier.addConservativeDelivery(seq);
            
            ConsTimelineCollector.sample = msg;
         }
      }
   }
   
   public static class OptimisticDeliverer extends Thread {
      MulticastServer mcServer;
      private SpeculativeDeliveryVerifier optVerifier;
      LatencyCalculator latencyCalculator;
      public OptimisticDeliverer(TestServer parent, SpeculativeDeliveryVerifier optVerifier, LatencyCalculator latencyCalculator) {
         super("OptimisticDeliverer");
         mcServer = parent.mcserver;
         this.optVerifier = optVerifier;
         this.latencyCalculator = latencyCalculator;
      }
      
      public void run() {
         while (true) {
            
            if (mcServer.getMulticastAgent() instanceof OptimisticMulticastAgent == false) {
               System.out.println("The Multicast agent " + mcServer.getMulticastAgent().getClass().getName() + " does not support uniform-opt deliveries");
               break;
            }
            
            OptimisticMulticastAgent omcagent = (OptimisticMulticastAgent) mcServer.getMulticastAgent();
            
            ClientMessage msg = (ClientMessage) omcagent.deliverMessageOptimistically();
            
            long now = System.currentTimeMillis();
            
            int  clientId  = msg.getSourceClientId();
            long mseq      = msg.getMessageSequence();
            long timestamp = (Long) msg.getNext();
            
            Message reply = new Message(mseq, DeliveryType.OPT);
            mcServer.sendReply(clientId, reply);

            long latency = now - timestamp;  
            latencyCalculator.addOptLatency(latency);
//            System.out.println(String.format("opt-delivered message %s within %d ms", mid, now - timestamp));
            optVerifier.addSpeculativeDelivery(mseq);
         }
      }
   }
   
   public static class FastDeliverer extends Thread {
      MulticastServer mcServer;
      SpeculativeDeliveryVerifier fastVerifier;
      LatencyCalculator latencyCalculator;
      public FastDeliverer(TestServer parent, SpeculativeDeliveryVerifier fastVerifier, LatencyCalculator latencyCalculator) {
         super("FastDeliverer");
         mcServer = parent.mcserver;
         this.fastVerifier = fastVerifier;
         this.latencyCalculator = latencyCalculator;
      }
      
      public void run() {
         while (true) {
            
            if (mcServer.getMulticastAgent() instanceof FastMulticastAgent == false) {
               System.out.println("The Multicast agent " + mcServer.getMulticastAgent().getClass().getName() + " does not support fast deliveries");
               break;
            }
            
            FastMulticastAgent fmcagent = (FastMulticastAgent) mcServer.getMulticastAgent();
            
            ClientMessage msg = (ClientMessage) fmcagent.deliverMessageFast();
            
            long now = System.currentTimeMillis();
            
            int  clientId  = msg.getSourceClientId();
            long mseq      = msg.getMessageSequence();
            long timestamp = (Long) msg.getNext();
            
            Message reply = new Message(mseq, DeliveryType.FAST);
            mcServer.sendReply(clientId, reply);

            long latency = now - timestamp;  
            latencyCalculator.addFastLatency(latency);
//            System.out.println(String.format("fast-delivered message %s within %d ms", mid, now - timestamp));
            fastVerifier.addSpeculativeDelivery(mseq);
         }
      }
   }   

   public static class ConsTimelineCollector extends Thread {
      public static Message sample = new Message();
//      private StatusPrinter printer;

      public ConsTimelineCollector(StatusPrinter printer) {
//         this.printer = printer;
      }

      @Override
      public void run() {
         long printInterval = 2500;
         while (true) {
            try {
               Thread.sleep(printInterval);
            } catch (InterruptedException e) {
               e.printStackTrace();
               System.exit(1);
            }

//            Message samp = sample;

//            long sendTime     = samp.t_coord_recv        - samp.t_client_send;
//            long coordOptTime = samp.t_coord_opt_merge   - samp.t_coord_recv;
//            long batchTime    = samp.t_batch_ready       - samp.t_coord_opt_merge;
//            long mcastTime    = samp.t_learner_received  - samp.t_batch_ready;
//            long mergeTime    = samp.t_learner_delivered - samp.t_learner_received;
//            long latency      = samp.t_learner_delivered - samp.t_client_send;
            
//            String consTimeline = String.format("T %d (s %d cm %d b %d mc %d lm %d)",
//                  latency, sendTime, coordOptTime, batchTime, mcastTime, mergeTime);
            
//            printer.print(this, consTimeline);
         }
      }
   }
   
   MulticastServer mcserver;
   ConservativeDeliverer consThread;
   OptimisticDeliverer    optThread;
   FastDeliverer         fastThread;
   
   LatencyCalculator latencyCalculator; 
   
   public TestServer(int serverId, String configFile) {
      mcserver = MulticastClientServerFactory.getServer(serverId, configFile);
      
      SpeculativeDeliveryVerifier optVerifier = new SpeculativeDeliveryVerifier(StatusPrinter.getInstance(), "opt");
      SpeculativeDeliveryVerifier fastVerifier = new SpeculativeDeliveryVerifier(StatusPrinter.getInstance(), "fast");
      FastDelInversionCollector inversioner = new FastDelInversionCollector(StatusPrinter.getInstance());
      ConsTimelineCollector consTimelineCollector = new ConsTimelineCollector(StatusPrinter.getInstance());
      consTimelineCollector.start();
      optVerifier.start();
      fastVerifier.start();
      
      if (mcserver.getMulticastAgent() instanceof RidgeMulticastAgent)
         inversioner.start();
      
      latencyCalculator = new LatencyCalculator(StatusPrinter.getInstance());
      latencyCalculator.start();
      consThread = new ConservativeDeliverer(this, optVerifier, fastVerifier, latencyCalculator);
      optThread  = new OptimisticDeliverer(this, optVerifier, latencyCalculator);
      fastThread = new FastDeliverer(this, fastVerifier, latencyCalculator);
   }
   
   public void start() {
      consThread.start();
      optThread.start();
      fastThread.start();
   }

   public static void main(String[] args) {
      /*

       one of the receivers should start with parameters:  9
         the other receiver should start with parameters: 10

       The parameter is the node id.
       Such node must be in the ridge configuration file, under the *group_members* section. This
       means that (for now) the whole system configuration is static, given in the config file.

      */
      
      int    nodeId     = Integer.parseInt(args[0]);
      String configFile = args[1];
      
      TestServer server = new TestServer(nodeId, configFile);
      server.start();
   }

}
