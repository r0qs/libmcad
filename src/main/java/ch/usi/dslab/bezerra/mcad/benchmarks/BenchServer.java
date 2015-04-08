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

package ch.usi.dslab.bezerra.mcad.benchmarks;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.math3.util.Pair;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.FastMulticastServer;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.mcad.MulticastServer;
import ch.usi.dslab.bezerra.mcad.ridge.RidgeMulticastServer;
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
         mrpmonitor = new MistakeRatePassiveMonitor(sid, "server", false);
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
         boolean isRidge = mcserver instanceof RidgeMulticastServer;
         int numGroupServers = -1;
         int serverIndex = -1;
         if (isRidge) {
            MulticastAgent mca = mcserver.getMulticastAgent();
            numGroupServers = mca.getLocalGroup().getMembers().size();
            serverIndex     = mca.getLocalGroup().getMembers().indexOf(mcserver.getId());
         }
         while (true) {
            ClientMessage consmsg = mcserver.deliverClientMessage();
            int  clientId = consmsg.getSourceClientId();
            long msgSeq   = consmsg.getMessageSequence();
            
            boolean optimistic = false;
            Message reply = new Message(msgSeq, optimistic);
            if (isRidge) {
               if (msgSeq % numGroupServers == serverIndex) {
                  mcserver.sendReply(clientId, reply);
               }
            }
            else if (mcserver.isConnectedToClient(clientId)) {
               mcserver.sendReply(clientId, reply);
            }
            OrderVerifier.instance.addConsDelivery(clientId, msgSeq);
         }
      }
   }
   
   public static class FastDeliverer extends Thread {
      FastMulticastServer fastmcserver;
      public FastDeliverer(MulticastServer parent) {
         if (fastmcserver instanceof FastMulticastServer)
            this.fastmcserver = (FastMulticastServer) parent;
         else {
            System.err.println(String.format("Provided MulticastServer does not implement FastMulticastServer (%s). Setting to null.", parent.getClass().getName()));
            this.fastmcserver = null;
         }
            
      }
      public void run() {
         if (fastmcserver == null) {
            System.err.println("Provided MulticastServer was set to null (given one does not implement FastMulticastServer)");
            return;
         }
         boolean isRidge = fastmcserver instanceof RidgeMulticastServer;
         int numGroupServers = -1;
         int serverIndex = -1;
         if (isRidge) {
            MulticastAgent mca = fastmcserver.getMulticastAgent();
            numGroupServers = mca.getLocalGroup().getMembers().size();
            serverIndex     = mca.getLocalGroup().getMembers().indexOf(fastmcserver.getId());
         }
         while (true) {
            ClientMessage optmsg = fastmcserver.deliverClientMessageFast();
            int  clientId = optmsg.getSourceClientId();
            long msgSeq   = optmsg.getMessageSequence();
            
            boolean optimistic = true;
            Message reply = new Message(msgSeq, optimistic);
            if (isRidge) {
               if (msgSeq % numGroupServers == serverIndex) {
                  fastmcserver.sendReply(clientId, reply);
               }
            }
            else if (fastmcserver.isConnectedToClient(clientId)) {
               fastmcserver.sendReply(clientId, reply);
            }
            OrderVerifier.instance.addOptDelivery(clientId, msgSeq);
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

   
   public boolean experimentTimeHasPassed(long start_ms) {
      return System.currentTimeMillis() > start_ms + DataGatherer.getDuration();
   }
   
   public BenchServer(int serverId, String config) {
      mcserver = MulticastClientServerFactory.getServer(serverId, config);
   }

   public static void main(String[] args) {
      int    sid          = Integer.parseInt(args[0]);
      String configFile   = args[1];
      String gathererHost = args[2];
      int    gathererPort = Integer.parseInt(args[3]);
      String logdir       = args[4];
      int    duration     = Integer.parseInt(args[5]);
      
      DataGatherer.configure(duration, logdir, gathererHost, gathererPort);
      
      BenchServer bs = new BenchServer(sid, configFile);
      bs.startRunning();
   }

}
