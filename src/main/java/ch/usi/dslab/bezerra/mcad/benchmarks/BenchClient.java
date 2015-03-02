package ch.usi.dslab.bezerra.mcad.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.sense.DataGatherer;
import ch.usi.dslab.bezerra.sense.monitors.LatencyDistributionPassiveMonitor;
import ch.usi.dslab.bezerra.sense.monitors.LatencyPassiveMonitor;
import ch.usi.dslab.bezerra.sense.monitors.ThroughputPassiveMonitor;

public class BenchClient implements Runnable {
   
   MulticastClient mcclient;
   AtomicInteger nextGroup = new AtomicInteger();
   AtomicLong nextMsgId = new AtomicLong();
   Map<Long, Long> optimisticStarts    = new ConcurrentHashMap<Long, Long>();
   Map<Long, Long> conservativeStarts  = new ConcurrentHashMap<Long, Long>();
   ThroughputPassiveMonitor optTPMonitor , consTPMonitor ;
   LatencyPassiveMonitor    optLatMonitor, consLatMonitor;
   LatencyDistributionPassiveMonitor  optLatDistMonitor, consLatDistMonitor;
   int msgSize;
   Semaphore permits;
   
   public  BenchClient (int clientId, String configFile, int msgSize, int numPermits) {
      ClientMessage.setGlobalClientId(clientId);
      this.msgSize = msgSize;
      mcclient = MulticastClientServerFactory.getClient(clientId, configFile);
      mcclient.connectToOneServerPerPartition();
//      for (Group g : Group.getAllGroups()) {
//         List<Integer> sids = g.getMembers();
//         int sindex = clientId % sids.size();
//         mcclient.connectToServer(sids.get(sindex));
//         System.out.println(String.format("Clientd %d connected to server %s", clientId, sids.get(sindex)));
//      }

      optTPMonitor   = new ThroughputPassiveMonitor(clientId, "optimistic", false);
      consTPMonitor  = new ThroughputPassiveMonitor(clientId, "conservative");
      optLatMonitor  = new LatencyPassiveMonitor(clientId, "optimistic", false);
      consLatMonitor = new LatencyPassiveMonitor(clientId, "conservative");
      optLatDistMonitor  = new LatencyDistributionPassiveMonitor(clientId, "optimistic", false);
      consLatDistMonitor = new LatencyDistributionPassiveMonitor(clientId, "conservative");
      
      System.out.println(String.format("Creating client %d with %d permits", clientId, numPermits));
      permits = new Semaphore(numPermits);
   }

   int getSendPermit() {
      permits.acquireUninterruptibly();
      return permits.availablePermits();
   }
   
   void addSendPermit() {      
      permits.release();
   }
   
   void sendMessage() {
      /* int num = */ getSendPermit();
//      System.out.println("permits: " + num);
      ClientMessage msg = new ClientMessage(new byte[msgSize]);
      
      List<Group> allGroups = Group.getAllGroups();
      int gid = nextGroup.incrementAndGet() % allGroups.size();
      Group g = allGroups.get(gid);
      List<Group> dests = new ArrayList<Group>(1);
      dests.add(g);
      
      long startTime = System.nanoTime();
      
      optimisticStarts  .put(msg.getMessageSequence(), startTime);
      conservativeStarts.put(msg.getMessageSequence(), startTime);
      
      mcclient.multicast(dests, msg);
   }

   
   @Override
   public void run() {
      long start = System.currentTimeMillis();
      long now = start;
      long end = start + DataGatherer.getDuration();
      while (now < end) {
         Message reply      = mcclient.deliverReply();
         long    reqId      = (Long) reply.getItem(0);
         boolean optimistic = (Boolean) reply.getItem(1);
         
         long nowNano = System.nanoTime();
         if (optimistic) {
            long sendTime = optimisticStarts.remove(reqId);
            long recvTime = nowNano;
            optTPMonitor.incrementCount();
            optLatMonitor.logLatency(sendTime, recvTime);
            optLatDistMonitor.logLatencyForDistribution(sendTime, recvTime);
//            System.out.println("opt-reply for message " + reqId);
         }
         else {
            addSendPermit();
            long sendTime = conservativeStarts.remove(reqId);
            long recvTime = nowNano;
            consTPMonitor.incrementCount();
            consLatMonitor.logLatency(sendTime, recvTime);
            consLatDistMonitor.logLatencyForDistribution(sendTime, recvTime);
//            System.out.println("cons-reply for message " + reqId);
         }
         
         now = System.currentTimeMillis();
      }
   }
   
   public static void checkParameters(String[] args) {
      if (args.length != 7) {
         System.err.println("usage: BenchClient clientId mcastConfigFile msgSize numPermits gathererHost gathererPort duration");
         System.exit(1);
      }
   }
   
   public boolean experimentTimeHasPassed(long start_ms) {
      return System.currentTimeMillis() > start_ms + DataGatherer.getDuration();
   }

   public static void main(String[] args) {

      checkParameters(args);
      
      // ======================== parameters
      int    clientId     = Integer.parseInt(args[0]);
      String configFile   = args[1];
      int    msgSize      = Integer.parseInt(args[2]);
      int    numPermits   = Integer.parseInt(args[3]);
      String gathererHost = args[4];
      int    gathererPort = Integer.parseInt(args[5]);
      int    duration     = Integer.parseInt(args[6]);
      // ===================================      

      DataGatherer.configure(duration, null, gathererHost, gathererPort);
      
      BenchClient cli = new BenchClient(clientId, configFile, msgSize, numPermits);
      
      Thread benchClientThread = new Thread(cli, "BenchClient");
      benchClientThread.start();
      
      while (true) {
         cli.sendMessage();
      }
   }
   
}
