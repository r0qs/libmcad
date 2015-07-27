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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastServer;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPConnection;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPMessage;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPReceiver;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPSender;

public class URPMulticastServer implements MulticastServer {
   
   public static interface MessageType {
      public final static int CLIENT_CREDENTIALS = 0;
   }
   
   public static class ConnectionListener implements Runnable {
      
      boolean running = true;
      URPMulticastServer parent;
      Thread clThread;
      
      public ConnectionListener(URPMulticastServer parent) {
         this.parent = parent;
         clThread = new Thread(this, "URPMulticastServer.ConnectionListener");
         clThread.start();
      }
      
      @Override
      public void run() {
         while (running) {
            TCPMessage newTcpMsg = parent.serverTcpReceiver.receive(1000);
            if (newTcpMsg == null)
               continue;
            else {
               System.out.println("New message received");
               TCPConnection connection = newTcpMsg.getConnection();
               Message contents = newTcpMsg.getContents();
               contents.rewind();
               int msgType = (Integer) contents.getNext();

               switch (msgType) {
                  case MessageType.CLIENT_CREDENTIALS: {
                     int clientId = (Integer) contents.getNext();
                     System.out.println("Client credentials: " + clientId);
                     parent.connectedClients.put(clientId, connection);
                     Message connectedAck = new Message("CONNECTED");
                     parent.sendReply(clientId, connectedAck);
                     break;
                  }
                  default:
                     break;
               }
            }
         }
      }
   }

   public static class URPMcastServerInfo {
      
      static Map<Integer, URPMcastServerInfo> serversMap = new ConcurrentHashMap<Integer, URPMcastServerInfo>();
      public static void addServerToMap(int serverId, int groupId, String host, int port) {
         serversMap.put(serverId, new URPMcastServerInfo(serverId, groupId, host, port));
      }
      public static URPMcastServerInfo getServer(int id) {
         return serversMap.get(id);
      }
      
      int serverId;
      int groupId;
      String host;
      int port;
      TCPConnection tcpConnection;
      
      public URPMcastServerInfo(int serverId, int groupId, String host, int port) {
         this.serverId = serverId;
         this.groupId  = groupId;
         this.host     = host;
         this.port     = port;
      }
      public int getId() {
         return serverId;
      }
      public String getHost() {
         return host;
      }
      public int getPort() {
         return port;
      }
      public TCPConnection getTCPConnection() {
         return tcpConnection;
      }
      public void setConnection(TCPConnection tcpConnection) {
         this.tcpConnection = tcpConnection;
      }
      
   }
   
   MulticastAgent  associatedMulticastAgent;
   URPAgentLearner associatedLearner;
   TCPSender   serverTcpSender;
   TCPReceiver serverTcpReceiver;
   Map<Integer, TCPConnection> connectedClients;
   ConnectionListener connectionListener;
   
   public URPMulticastServer(MulticastAgent associatedAgent, URPAgentLearner associatedLearner, int port) {
      this.associatedMulticastAgent = associatedAgent;
      this.associatedLearner = associatedLearner;
      this.serverTcpSender = new TCPSender();
      this.serverTcpReceiver = new TCPReceiver(port);
      this.connectedClients = new ConcurrentHashMap<Integer, TCPConnection>();
      this.connectionListener = new ConnectionListener(this);
   }

   @Override
   public int getId() {
      return associatedLearner.getLearnerId();
   }

   @Override
   public boolean isConnectedToClient(int clientId) {
      return connectedClients.containsKey(clientId);
   }

   @Override
   public void sendReply(int clientId, Message reply) {
      TCPConnection clientConnection = connectedClients.get(clientId);
      if (clientConnection != null) serverTcpSender.send(reply, clientConnection);
   }

   @Override
   public MulticastAgent getMulticastAgent() {
      return associatedMulticastAgent;
   }

   @Override
   public ClientMessage deliverClientMessage() {
      Message msg = associatedMulticastAgent.deliverMessage();
      if (msg instanceof ClientMessage)
         return (ClientMessage) msg;
      else {
         System.err.println("msg not instance of ClientMessage");
         System.exit(1);
         return null;
      }
   }

}
