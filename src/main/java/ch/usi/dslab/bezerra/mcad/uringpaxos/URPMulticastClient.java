package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
      mcagent = new URPMcastAgent(configFile, false);
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
         System.out.println(String.format("Client %d: %s", this.getId(), ack.getItem(0)));
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
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
   public void multicast(List<Group> destinations, Message msg) {
      mcagent.multicast(destinations, msg);
   }

}
