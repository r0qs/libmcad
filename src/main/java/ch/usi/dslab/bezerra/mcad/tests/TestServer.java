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

import ch.usi.dslab.bezerra.mcad.FastMulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.mcad.MulticastServer;
import ch.usi.dslab.bezerra.mcad.OptimisticMulticastAgent;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.Merger;
import ch.usi.dslab.bezerra.ridge.RidgeMessage.MessageIdentifier;

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
      private DescriptiveStatistics consStats = new DescriptiveStatistics();
      private DescriptiveStatistics  optStats = new DescriptiveStatistics();
      private DescriptiveStatistics fastStats = new DescriptiveStatistics();
      
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
                  "latencies: c: [%.1f - %.1f - %.1f] avg: %.2f\n" +
                  "           o: [%.1f - %.1f - %.1f] avg: %.2f\n" +
                  "           f: [%.1f - %.1f - %.1f] avg: %.2f",
                  consStats.getPercentile(25), consStats.getPercentile(50), consStats.getPercentile(75), consStats.getMean(),
                   optStats.getPercentile(25),  optStats.getPercentile(50),  optStats.getPercentile(75),  optStats.getMean(),
                  fastStats.getPercentile(25), fastStats.getPercentile(50), fastStats.getPercentile(75), fastStats.getMean());
            
            printer.print(this, latencyLine);
         }
      }
   }
   
   public static class SpeculativeDeliveryVerifier extends Thread {
      public List<MessageIdentifier> speculativeSequence  = new LinkedList<MessageIdentifier>();
      public List<MessageIdentifier> conservativeSequence = new LinkedList<MessageIdentifier>();
      private StatusPrinter printer;
      private String name;
      private AtomicLong numDeliveries = new AtomicLong(0);
      private AtomicLong mistakes = new AtomicLong(0);
      
      public SpeculativeDeliveryVerifier(StatusPrinter printer, String name) {
         super("SpeculativeDeliveryVerifier");
         this.printer = printer;
         this.name    = name;
      }
      
      synchronized public void addSpeculativeDelivery(MessageIdentifier mid) {
         speculativeSequence.add(mid);
         checkMatch();
      }
      
      synchronized public void addConservativeDelivery(MessageIdentifier mid) {
         conservativeSequence.add(mid);
         numDeliveries.incrementAndGet();
         checkMatch();
      }
      
      synchronized private void checkMatch() {
         while (speculativeSequence.isEmpty() == false && conservativeSequence.isEmpty() == false) {
            MessageIdentifier smid = speculativeSequence.remove(0);
            MessageIdentifier cmid = conservativeSequence.remove(0);
            if (smid.equals(cmid) == false) mistakes.incrementAndGet();
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
            String status = String.format("mistakes(%s): %d/%d = %.2f%%", name, m, d, rate);
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
      List<MessageIdentifier> allDeliveries = new ArrayList<MessageIdentifier>();
      ListHashCalculator         allDeliveriesHashPrinter = new ListHashCalculator(StatusPrinter.getInstance(), "all", allDeliveries);
      Map<String, List<MessageIdentifier>> receivedMessages = new ConcurrentHashMap<String, List<MessageIdentifier>>();
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
      
      void addDelivery(String destsStr, MessageIdentifier mid) {
         List<MessageIdentifier> destMsgs = receivedMessages.get(destsStr);
         if (destMsgs == null) {
            destMsgs = new ArrayList<MessageIdentifier>();
            receivedMessages.put(destsStr, destMsgs);
            ListHashCalculator lhp = new ListHashCalculator(StatusPrinter.getInstance(), destsStr, destMsgs);
            lhp.start();
         }
         synchronized (destMsgs) {
            destMsgs.add(mid);
         }
         synchronized (allDeliveries) {
            allDeliveries.add(mid);
         }
      }
      
      public void run() {
         while (true) {
            Message msg = mcServer.getMulticastAgent().deliverMessage();
            long now = System.currentTimeMillis();
            
            int clientId = (Integer) msg.getNext();
            MessageIdentifier mid = (MessageIdentifier) msg.getNext();
            long timestamp = (Long) msg.getNext();
            String destinationString = (String) msg.getNext();
            
            addDelivery(destinationString, mid);
            
            Message reply = new Message(mid, DeliveryType.CONS);
            mcServer.sendReply(clientId, reply);

            long latency = now - timestamp;  
            latencyCalculator.addConsLatency(latency);
//            System.out.println(String.format("cons-delivered message %s within %d ms", mid, now - timestamp));
            optVerifier.addConservativeDelivery(mid);
            fastVerifier.addConservativeDelivery(mid);
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
            OptimisticMulticastAgent omcagent = (OptimisticMulticastAgent) mcServer.getMulticastAgent();
            
            Message msg = omcagent.deliverMessageOptimistically();
            
            long now = System.currentTimeMillis();
            
            int clientId = (Integer) msg.getNext();
            MessageIdentifier mid = (MessageIdentifier) msg.getNext();
            long timestamp = (Long) msg.getNext();
            
            Message reply = new Message(mid, DeliveryType.OPT);
            mcServer.sendReply(clientId, reply);

            long latency = now - timestamp;  
            latencyCalculator.addOptLatency(latency);
//            System.out.println(String.format("opt-delivered message %s within %d ms", mid, now - timestamp));
            optVerifier.addSpeculativeDelivery(mid);
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
            FastMulticastAgent fmcagent = (FastMulticastAgent) mcServer.getMulticastAgent();
            
            Message msg = fmcagent.deliverMessageFast();
            
            long now = System.currentTimeMillis();
            
            int clientId = (Integer) msg.getNext();
            MessageIdentifier mid = (MessageIdentifier) msg.getNext();
            long timestamp = (Long) msg.getNext();
            
            Message reply = new Message(mid, DeliveryType.FAST);
            mcServer.sendReply(clientId, reply);

            long latency = now - timestamp;  
            latencyCalculator.addFastLatency(latency);
//            System.out.println(String.format("fast-delivered message %s within %d ms", mid, now - timestamp));
            fastVerifier.addSpeculativeDelivery(mid);
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
      optVerifier.start();
      fastVerifier.start();
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
