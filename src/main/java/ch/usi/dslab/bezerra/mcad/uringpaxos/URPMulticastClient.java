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

package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.mcad.uringpaxos.URPMulticastServer.MessageType;
import ch.usi.dslab.bezerra.mcad.uringpaxos.URPMulticastServer.URPMcastServerInfo;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPConnection;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPMessage;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPReceiver;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPSender;

public class URPMulticastClient implements MulticastClient, Runnable {
   
   private int clientId;
   TCPSender clientTCPSender;
   TCPReceiver clientTCPReceiver;
   BlockingQueue<Message> receivedReplies;
   MulticastAgent mcagent;
   Thread receivingThread;

   public URPMulticastClient(int clientId, String configFile) {
      this.clientId = clientId;
      clientTCPSender = new TCPSender();
      clientTCPReceiver = new TCPReceiver();
      receivedReplies = new LinkedBlockingQueue<Message>();
      receivingThread = new Thread(this, "URPMulticastClient");
      receivingThread.start();
      mcagent = new URPMcastAgent(configFile);
   }

   public int getId() {
      return clientId;
   }
   
   @Override
   public void run() {
      while(true) {
         TCPMessage tcpmsg = clientTCPReceiver.receive(1000);
         if (tcpmsg == null) continue;
         
         Message msg = tcpmsg.getContents();
         receivedReplies.add(msg);
      }
   }

   @Override
   public void connectToServer(int serverId) {

      try {
         // retrieve server info
         URPMcastServerInfo serverInfo = URPMcastServerInfo.getServer(serverId);

         // establish tcp connection, associate it with the server info and add to receiver
         TCPConnection serverConnection = new TCPConnection(serverInfo.getHost(), serverInfo.getPort());
         serverInfo.setConnection(serverConnection);
         clientTCPReceiver.addConnection(serverConnection);

         // send credentials message
         Message credentials = new Message(MessageType.CLIENT_CREDENTIALS, this.getId());
         clientTCPSender.send(credentials, serverConnection);

         Message ack = deliverReply();
         System.out.println(String.format("Client %d connecting to server %d. Server's reply: %s", this.getId(), serverId, ack.getItem(0)));
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

   }
   
   @Override
   public void connectToOneServerPerPartition() {
      final List<Group> groups = URPGroup.getAllGroups();
      for (Group group : groups) {
         List<Integer> groupMembers = group.getMembers();
         int chosenServerId = groupMembers.get(clientId % groupMembers.size());
         connectToServer(chosenServerId);
      }
   }
   
   void send(Message msg, int serverId) {
      URPMcastServerInfo serverInfo = URPMcastServerInfo.getServer(serverId);
      send(msg, serverInfo);
   }
   
   void send(Message msg, URPMcastServerInfo server) {
      if (server.tcpConnection == null) connectToServer(server.getId());
      clientTCPSender.send(msg, server.getTCPConnection());
   }
   
   @Override
   public Message deliverReply() {
      try {
         return receivedReplies.take();
      } catch (InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return null;
   }

   @Override
   public void multicast(List<Group> destinations, ClientMessage msg) {
      mcagent.multicast(destinations, msg);
   }

}
