package ch.usi.dslab.bezerra.mcad.ridge;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.Client;
import ch.usi.dslab.bezerra.ridge.RidgeMessage;


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
   public void multicast(List<Group> destinations, Message message) {
      ridgeMulticastAgent.multicast(destinations, message);
   }

}
