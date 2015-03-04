package ch.usi.dslab.bezerra.mcad.benchmarks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.mcad.benchmarks.BenchmarkEventList.MessageEvent;
import ch.usi.dslab.bezerra.mcad.benchmarks.BenchmarkEventList.PermitEvent;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.netwrapper.codecs.Codec;
import ch.usi.dslab.bezerra.netwrapper.codecs.CodecGzip;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPConnection;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPSender;

public class DynamicBenchClient extends BenchClient {

   Map<Long, Long> sendTimes  = new ConcurrentHashMap<Long, Long>();
   BenchmarkEventList eventList = new BenchmarkEventList();
   
   long durationMS;
   int initialLoad;
   int finalLoad;
   Thread clientThread, loadIncreaserThread, logSenderThread;
   String gathererHost;
   int    gathererPort;
   
   private static class LoadIncreaser implements Runnable {
      DynamicBenchClient client;
      int currentPermits;
      public LoadIncreaser(DynamicBenchClient parent) {
         client = parent;
         currentPermits = client.getNumSendPermits();
      }
      public void run() {
         long startTime = System.currentTimeMillis();
         while (true) {
            try {
               Thread.sleep(500);
            } catch (InterruptedException e) {
               e.printStackTrace();
               System.exit(1);
            }
            long now = System.currentTimeMillis();
            long elapsedTime = now - startTime;
            int expectedPermits = (int) (Math.round(((double) elapsedTime / (double) client.durationMS)) * (double) client.finalLoad);
            if (expectedPermits > currentPermits) {
               client.addSendPermit(expectedPermits - currentPermits);
               currentPermits = expectedPermits;
               client.eventList.addEvent(new PermitEvent(now, client.clientId, expectedPermits));
            }
         }
      }
   }
   
   public static class LogSender implements Runnable {
      DynamicBenchClient client;
      long duration;
      public LogSender(DynamicBenchClient client, long duration) {
         this.client   = client;
         this.duration = duration;
      }
      public void run() {
         try {
            Codec gzip = new CodecGzip();
            Thread.sleep(duration);
            client.eventList.stopLogging();
            Message msg = new Message(gzip.getBytes(client.eventList));
            TCPSender tcp = new TCPSender();
            TCPConnection gathererConnection = new TCPConnection(client.gathererHost, client.gathererPort);
            tcp.send(msg, gathererConnection);

         } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            System.exit(1);
         }
      }
   }
   
   public DynamicBenchClient(int clientId, String configFile, int msgSize, long durationMS, int initialLoad, int finalLoad, String gathererHost, int gathererPort) {
      ClientMessage.setGlobalClientId(clientId);
      this.msgSize = msgSize;
      mcclient = MulticastClientServerFactory.getClient(clientId, configFile);
      mcclient.connectToOneServerPerPartition();

      System.out.println(String.format("Creating dynamic client %d with function (%d s, p0 %d, pf %d)", clientId, durationMS/1000, initialLoad, finalLoad));
      permits = new Semaphore(initialLoad);
      this.durationMS  = durationMS;
      this.initialLoad = initialLoad;
      this.finalLoad   = finalLoad;
      this.gathererHost = gathererHost;
      this.gathererPort = gathererPort;
   }
   
   @Override
   public void run() {
      while (true) {
         Message reply      = mcclient.deliverReply();
         long    reqId      = (Long) reply.getItem(0);
         boolean optimistic = (Boolean) reply.getItem(1);
         
         long nowMilli = System.currentTimeMillis();
         long nowNano  = System.nanoTime();
         if (optimistic) {
         }
         else {
            addSendPermit();
            long sendTime = sendTimes.remove(reqId);
            long recvTime = nowNano;
            long latencyNano = recvTime - sendTime;
            eventList.addEvent(new MessageEvent(nowMilli, latencyNano));
         }
      }
   }
   
   void sendMessage() {
      getSendPermit();
      ClientMessage msg = new ClientMessage(new byte[msgSize]);
      
      List<Group> allGroups = Group.getAllGroups();
      int gid = nextGroup.incrementAndGet() % allGroups.size();
      Group g = allGroups.get(gid);
      List<Group> dests = new ArrayList<Group>(1);
      dests.add(g);
      
      long sendTime = System.nanoTime();
      
      sendTimes.put(msg.getMessageSequence(), sendTime);
      
      mcclient.multicast(dests, msg);
   }
   
   public void execute() {
      clientThread        = new Thread(this, "DynamicBenchClient");
      loadIncreaserThread = new Thread(new LoadIncreaser(this), "LoadIncreaser");
      logSenderThread     = new Thread(new LogSender(this, durationMS), "LogSender");
      clientThread       .start();
      loadIncreaserThread.start();
      logSenderThread    .start();
      while (true)
         sendMessage();
   }
   
   public static void checkParameters(String[] args) {
      if (args.length != 8) {
         System.err.println("usage: DynamicBenchClient clientId mcastConfigFile msgSize gathererHost gathererPort duration initialLoad finalLoad");
         System.exit(1);
      }
   }
   
   public static void main(String[] args) {

      checkParameters(args);
      
      // ======================== parameters
      int    clientId        = Integer.parseInt(args[0]);
      String configFile      = args[1];
      int    msgSize         = Integer.parseInt(args[2]);
      String dynGathererHost = args[3];
      int    dynGathererPort = Integer.parseInt(args[4]);
      long   durationMS      = Integer.parseInt(args[5]) * 1000L;
      int    initialLoad     = Integer.parseInt(args[6]);
      int    finalLoad       = Integer.parseInt(args[7]);
      // ===================================      

      DynamicBenchClient dc = new DynamicBenchClient(clientId, configFile, msgSize, durationMS, initialLoad, finalLoad, dynGathererHost, dynGathererPort);
      dc.execute();
   }

}
