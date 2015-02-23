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
      public static void addServerToMap(int serverId, String host, int port) {
         serversMap.put(serverId, new URPMcastServerInfo(serverId, host, port));
      }
      public static URPMcastServerInfo getServer(int id) {
         return serversMap.get(id);
      }
      
      int id;
      String host;
      int port;
      TCPConnection tcpConnection;
      public URPMcastServerInfo(int id, String host, int port) {
         this.id   = id;
         this.host = host;
         this.port = port;
      }
      public int getId() {
         return id;
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
