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
         List<Integer> groupMembers = group.getMembers();
         int chosenServerId = groupMembers.get(getPid() % groupMembers.size());
         connectToServer(chosenServerId);
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
