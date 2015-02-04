package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastServer;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPConnection;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPMessage;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPReceiver;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPSender;

public class URPMulticastServer implements MulticastServer {
   
   public static class ConnectionListener implements Runnable {

      public final static int CLIENT_CREDENTIALS = 0;
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
               TCPConnection connection = newTcpMsg.getConnection();
               Message contents = (Message) newTcpMsg.getContents();
               contents.rewind();
               int msgType = (Integer) contents.getNext();

               switch (msgType) {
                  case CLIENT_CREDENTIALS: {
                     int clientId = (Integer) contents.getNext();
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

   MulticastAgent  associatedMulticastAgent;
   URPAgentLearner associatedLearner;
   TCPSender   serverTcpSender;
   TCPReceiver serverTcpReceiver;
   Map<Integer, TCPConnection> connectedClients;
   
   public URPMulticastServer(MulticastAgent associatedAgent, URPAgentLearner associatedLearner, int port) {
      this.associatedMulticastAgent = associatedAgent;
      this.associatedLearner = associatedLearner;
      this.serverTcpSender = new TCPSender();
      this.serverTcpReceiver = new TCPReceiver(port);
      this.connectedClients = new ConcurrentHashMap<Integer, TCPConnection>();
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
      serverTcpSender.send(reply, clientConnection);
   }

   @Override
   public MulticastAgent getMulticastAgent() {
      return associatedMulticastAgent;
   }

}
