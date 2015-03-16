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

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.Client;


public class RidgeMulticastClient extends Client implements MulticastClient {
   
   RidgeMulticastAgent ridgeMulticastAgent;
   
   BlockingQueue<Message> receivedReplies;
   
   public RidgeMulticastClient(int id, RidgeMulticastAgent rmcAgent) {
      super(id);
      this.ridgeMulticastAgent = rmcAgent;
      receivedReplies = new LinkedBlockingDeque<Message>();
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
      ridgeMulticastAgent.multicast(destinations, message);
   }

}
