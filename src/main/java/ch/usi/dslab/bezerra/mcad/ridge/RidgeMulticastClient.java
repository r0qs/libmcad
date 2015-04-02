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

package ch.usi.dslab.bezerra.mcad.ridge;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.util.Pair;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.Client;


public class RidgeMulticastClient extends Client implements MulticastClient {
   
   public static class RequestBatcher extends Thread {

      private RidgeMulticastClient parentClient;
      private Message currentBatch;
      private int currentBatchLength_Bytes;
      private List<Group> currentDestinations;
      private BlockingQueue<Pair<List<Group>, Message>> readyBatches;
      private long lastSendingTime;
      private static long clientBatchTimeout_ms;
      private static int  clientBatchSize_Bytes;
      
      public static long getClientBatchTimeout_ms() {
         return clientBatchTimeout_ms;
      }

      public static void setClientBatchTimeout_ms(long clientBatchTimeout_ms) {
         RequestBatcher.clientBatchTimeout_ms = clientBatchTimeout_ms;
      }
      
      public static int getClientBatchSize_Bytes() {
         return clientBatchSize_Bytes;
      }

      public static void setClientBatchSize_Bytes(int clientBatchSize_Bytes) {
         RequestBatcher.clientBatchSize_Bytes = clientBatchSize_Bytes;
      }

      public RequestBatcher(RidgeMulticastClient parentClient) {
         super("RequestBatcher");
         this.parentClient = parentClient;
         this.currentDestinations = null;
         this.currentBatch = new Message();
         this.readyBatches = new LinkedBlockingQueue<Pair<List<Group>, Message>>();
         this.lastSendingTime = System.currentTimeMillis();
      }
      
      synchronized private void closeCurrentBatch() throws InterruptedException {
         Pair<List<Group>, Message> readyBatch = new Pair<List<Group>, Message>(currentDestinations, currentBatch);
         readyBatches.put(readyBatch);
         currentBatch = new Message();
         currentDestinations = null;
         currentBatchLength_Bytes = 0;
      }
      
      synchronized private boolean isSendingTime() {
         if (currentBatch.count() == 0)
            return false;
         if (currentBatchLength_Bytes >= clientBatchSize_Bytes)
            return true;
         long now = System.currentTimeMillis();
         if (now - lastSendingTime >= clientBatchTimeout_ms) {
            lastSendingTime = now;
            return true;
         }
         return false;
      }
      
      synchronized private void checkBatchThresholds() throws InterruptedException {
         if (isSendingTime()) {
            closeCurrentBatch();
         }
      }
      
      synchronized public void addRequest(List<Group> destinations, ClientMessage request) {
         try {
            if (currentDestinations != null && currentDestinations.equals(destinations) == false) {
               closeCurrentBatch();
            }
            currentBatchLength_Bytes += request.packContents();
            currentBatch.addItems(request);
            currentDestinations = destinations;
            checkBatchThresholds();
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
      
      @Override
      public void run() {
         try {
            while (true) {
               Pair<List<Group>, Message> nextBatch = readyBatches.poll(clientBatchTimeout_ms, TimeUnit.MILLISECONDS);
               checkBatchThresholds();
               if (nextBatch != null) {
                  parentClient.ridgeMulticastAgent.multicast(nextBatch.getFirst(), nextBatch.getSecond());
               }
               
            }
         }
         catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
      
   }
   
   RidgeMulticastAgent ridgeMulticastAgent;
   BlockingQueue<Message> receivedReplies;
   RequestBatcher requestBatcher;
   
   public RidgeMulticastClient(int id, RidgeMulticastAgent rmcAgent) {
      super(id);
      this.ridgeMulticastAgent = rmcAgent;
      this.receivedReplies     = new LinkedBlockingDeque<Message>();
      this.requestBatcher      = new RequestBatcher(this);
   }
   
   @Override
   public void connectToServer(int serverId) {
      connectToLearner(serverId);
      Message ack = deliverReply();
      System.out.println(String.format("Client %d: %s", this.getPid(), ack.getItem(0)));
   }
   
   @Override
   public void connectToOneServerPerPartition() {
      final List<Group> groups = Group.getAllGroups();
      for (Group group : groups) {
         final List<Integer> groupMembers = group.getMembers();
         int chosenServerId = groupMembers.get(getPid() % groupMembers.size());
         connectToServer(chosenServerId);
      }
   }
   
   public void connectToAllServers() {
      final List<Group> groups = Group.getAllGroups();
      for (Group group : groups) {
         final List<Integer> groupMembers = group.getMembers();
         for (int groupMemberId : groupMembers)
            connectToServer(groupMemberId);            
      }
   }
   
   @Override
   public void uponDelivery(Message reply) {
      receivedReplies.add(reply);
   }

   @Override
   public Message deliverReply() {
      Message delivery = null;
      try {
         delivery = receivedReplies.take();
         delivery.rewind();
      } catch (InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return delivery;
   }

   @Override
   public void multicast(List<Group> destinations, ClientMessage message) {
      requestBatcher.addRequest(destinations, message);
   }

}
