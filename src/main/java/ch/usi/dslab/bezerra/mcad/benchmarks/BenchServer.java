package ch.usi.dslab.bezerra.mcad.benchmarks;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.math3.util.Pair;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.FastMulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.mcad.MulticastServer;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.sense.DataGatherer;
import ch.usi.dslab.bezerra.sense.monitors.MistakeRatePassiveMonitor;

public class BenchServer {
   MulticastServer mcserver;
   
   public static class OrderVerifier {
      public static OrderVerifier instance;
      Queue<Pair<Integer, Long>>  optDeliveries = new ConcurrentLinkedQueue<Pair<Integer, Long>>();
      Queue<Pair<Integer, Long>> consDeliveries = new ConcurrentLinkedQueue<Pair<Integer, Long>>();
      MistakeRatePassiveMonitor mrpmonitor;
      
      public OrderVerifier(int sid) {
         DataGatherer.configure(60, null, "node40", 60000);
         mrpmonitor = new MistakeRatePassiveMonitor(sid, "server");
         instance = this;
      }
      
      synchronized void addOptDelivery(int clientId, long seq) {
         optDeliveries.add(new Pair<Integer, Long>(clientId, seq));
         match();
      }
      synchronized void addConsDelivery(int clientId, long seq) {
         consDeliveries.add(new Pair<Integer, Long>(clientId, seq));
         match();
      }
      synchronized void match() {
         while (optDeliveries.isEmpty() == false && consDeliveries.isEmpty() == false) {
            Pair<Integer, Long> optid  =  optDeliveries.poll();
            Pair<Integer, Long> consid = consDeliveries.poll();
            if (optid.equals(consid))
               mrpmonitor.incrementCounts(1, 0);
            else
               mrpmonitor.incrementCounts(1, 1);
         }
      }
   }
   
   public static class ConservativeDeliverer extends Thread {
      MulticastServer mcserver;
      public ConservativeDeliverer(MulticastServer parent) {
         this.mcserver = parent;
      }
      public void run() {
         long now, start, end;
         now = start = System.currentTimeMillis();
         end = start + DataGatherer.getDuration();
         while (now < end) {
            ClientMessage consmsg = mcserver.deliverClientMessage();
            int  clientId = consmsg.getSourceClientId();
            long msgSeq   = consmsg.getMessageSequence();
            boolean optimistic = false;
            Message reply = new Message(msgSeq, optimistic);
            mcserver.sendReply(clientId, reply);
            OrderVerifier.instance.addConsDelivery(clientId, msgSeq);
//            System.out.println("cons-delivered message " + clientId + "." + msgSeq);
            now = System.currentTimeMillis();
         }
      }
   }
   
   public static class FastDeliverer extends Thread {
      MulticastServer mcserver;
      public FastDeliverer(MulticastServer parent) {
         this.mcserver = parent;
      }
      public void run() {
         long now, start, end;
         now = start = System.currentTimeMillis();
         end = start + DataGatherer.getDuration();
         FastMulticastAgent omca = (FastMulticastAgent) mcserver.getMulticastAgent();
         while (now < end) {
            ClientMessage optmsg = (ClientMessage) omca.deliverMessageFast();
            int  clientId = optmsg.getSourceClientId();
            long msgSeq   = optmsg.getMessageSequence();
            boolean optimistic = true;
            Message reply = new Message(msgSeq, optimistic);
            mcserver.sendReply(clientId, reply);
            OrderVerifier.instance.addOptDelivery(clientId, msgSeq);
//            System.out.println("opt-delivered message " + clientId + "." + msgSeq);
            now = System.currentTimeMillis();
         }
      }
   }
   
   public void startRunning() {
      Thread consDeliverer = new Thread(new ConservativeDeliverer(mcserver), "ConsDeliverer");
      Thread optDeliverer  = new Thread(new FastDeliverer(mcserver), "OptDeliverer");
      new OrderVerifier(mcserver.getId());
      consDeliverer.start();
      optDeliverer.start();
   }
   
   public BenchServer(int serverId, String config) {
      mcserver = MulticastClientServerFactory.getServer(serverId, config);
   }

   public static void main(String[] args) {
      int sid = Integer.parseInt(args[0]);
      String configFile = args[1];
      BenchServer bs = new BenchServer(sid, configFile);
      bs.startRunning();
   }

}
