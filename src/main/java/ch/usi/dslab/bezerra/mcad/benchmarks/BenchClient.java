package ch.usi.dslab.bezerra.mcad.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.sense.DataGatherer;
import ch.usi.dslab.bezerra.sense.monitors.LatencyPassiveMonitor;
import ch.usi.dslab.bezerra.sense.monitors.ThroughputPassiveMonitor;

public class BenchClient implements Runnable {
   
   public static class BenchMessage extends Message {
      private static final long serialVersionUID = 1L;
      public static int globalCliId;
      private static AtomicLong nextMsgSeq = new AtomicLong();
      int cliId;
      long seq;
      public BenchMessage() {super();}
      public BenchMessage(Object... objs) {
         super(objs);
         cliId = globalCliId;
         seq = nextMsgSeq.incrementAndGet();
      }
   }
   
   MulticastClient mcclient;
   AtomicInteger nextGroup = new AtomicInteger();
   AtomicLong nextMsgId = new AtomicLong();
   Map<Long, Long> optimisticStarts    = new ConcurrentHashMap<Long, Long>();
   Map<Long, Long> conservativeStarts  = new ConcurrentHashMap<Long, Long>();
   LatencyPassiveMonitor    optLatMonitor, consLatMonitor;
   ThroughputPassiveMonitor optTPMonitor , consTPMonitor ;
   int msgSize;
   Semaphore permits;
   
   public  BenchClient (int clientId, String configFile, int msgSize, int numPermits) {
      BenchMessage.globalCliId = clientId;
      this.msgSize = msgSize;
      mcclient = MulticastClientServerFactory.getClient(clientId, configFile);
      mcclient.connectToOneServerPerPartition();
//      for (Group g : Group.getAllGroups()) {
//         List<Integer> sids = g.getMembers();
//         int sindex = clientId % sids.size();
//         mcclient.connectToServer(sids.get(sindex));
//         System.out.println(String.format("Clientd %d connected to server %s", clientId, sids.get(sindex)));
//      }
      
      optLatMonitor  = new LatencyPassiveMonitor(clientId, "optimistic", false);
      consLatMonitor = new LatencyPassiveMonitor(clientId, "conservative");
      optTPMonitor   = new ThroughputPassiveMonitor(clientId, "optimistic", false);
      consTPMonitor  = new ThroughputPassiveMonitor(clientId, "conservative");
      
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
      BenchMessage msg = new BenchMessage(new byte[msgSize]);
      
      List<Group> allGroups = Group.getAllGroups();
      int gid = nextGroup.incrementAndGet() % allGroups.size();
      Group g = allGroups.get(gid);
      List<Group> dests = new ArrayList<Group>(1);
      dests.add(g);
      
      long startTime = System.nanoTime();
      
      optimisticStarts  .put(msg.seq, startTime);
      conservativeStarts.put(msg.seq, startTime);
      
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
            optLatMonitor.logLatency(sendTime, recvTime);
            optTPMonitor.incrementCount();
//            System.out.println("opt-reply for message " + reqId);
         }
         else {
            addSendPermit();
            long sendTime = conservativeStarts.remove(reqId);
            long recvTime = nowNano;
            consLatMonitor.logLatency(sendTime, recvTime);
            consTPMonitor.incrementCount();
//            System.out.println("cons-reply for message " + reqId);
         }
         
         now = System.currentTimeMillis();
      }
   }
   
   public static void main(String[] args) {

      // ======================== parameters
      int cid = Integer.parseInt(args[0]);
      String configFile = args[1];
      int msgSize = Integer.parseInt(args[2]);
      int numPermits = Integer.parseInt(args[3]);
      // ===================================      

      DataGatherer.configure(60, null, "node40", 60000);
      
      BenchClient cli = new BenchClient(cid, configFile, msgSize, numPermits);
      
      Thread benchClientThread = new Thread(cli, "BenchClient");
      benchClientThread.start();
      
      long start = System.currentTimeMillis();
      long now = start;
      long end = start + DataGatherer.getDuration();
      while (now < end) {
         cli.sendMessage();
         now = System.currentTimeMillis();
      }
   }
   
}
