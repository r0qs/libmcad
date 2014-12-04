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
      public BenchMessage(Object... objs) {
         super(objs);
         this.cliId = globalCliId;
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
   Semaphore permits = new Semaphore(1);
   
   public  BenchClient (int clientId, String configFile, int msgSize) {
      BenchMessage.globalCliId = clientId;
      this.msgSize = msgSize;
      mcclient = MulticastClientServerFactory.getClient(clientId, configFile);
      for (Group g : Group.getAllGroups()) {
         List<Integer> sids = g.getMembers();
         int sindex = clientId % sids.size();
         mcclient.connectToServer(sids.get(sindex));
      }
      
      DataGatherer.configure(60, null, "node41", 60000);
      
      optLatMonitor  = new LatencyPassiveMonitor(clientId, "optimistic");
      consLatMonitor = new LatencyPassiveMonitor(clientId, "conservative");
      optTPMonitor   = new ThroughputPassiveMonitor(clientId, "optimistic");
      consTPMonitor  = new ThroughputPassiveMonitor(clientId, "conservative");
   }

   void getSendPermit() {
      permits.acquireUninterruptibly();
   }
   
   void addSendPermit() {      
      permits.release();
   }
   
   void sendMessage() {
      getSendPermit();
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
         
         now = System.nanoTime();
         if (optimistic) {
            long sendTime = optimisticStarts.get(reqId);
            long recvTime = now;
            optLatMonitor.logLatency(sendTime, recvTime);
            optTPMonitor.incrementCount();
         }
         else {
            addSendPermit();
            long sendTime = conservativeStarts.get(reqId);
            long recvTime = now;
            consLatMonitor.logLatency(sendTime, recvTime);
            consTPMonitor.incrementCount();
         }
      }
   }
   
   public static void main(String[] args) {

      // ======================== parameters
      int cid = Integer.parseInt(args[0]);
      String configFile = args[1];
      int msgSize = Integer.parseInt(args[2]);
      // ===================================      
      
      BenchClient cli = new BenchClient(cid, configFile, msgSize);
      
      Thread benchClientThread = new Thread(cli, "BenchClient");
      benchClientThread.start();
      
      DataGatherer.configure(60, null, "node41", 60000);
      
      long start = System.currentTimeMillis();
      long now = start;
      long end = start + DataGatherer.getDuration();
      while (now < end) {
         cli.sendMessage();
         now = System.currentTimeMillis();
      }
   }
   
}
