package ch.usi.dslab.bezerra.mcad.ridge;

import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastServer;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.Learner;

public class RidgeMulticastServer implements MulticastServer {
   
   MulticastAgent associatedMulticastAgent;
   Learner associatedLearner;

   public RidgeMulticastServer(MulticastAgent associatedAgent, Learner associatedLearner) {
      this.associatedMulticastAgent = associatedAgent;
      this.associatedLearner = associatedLearner;
   }
   
   @Override
   public int getId() {
      return associatedLearner.getPid();
   }
   
   @Override
   public boolean isConnectedToClient(int clientId) {
      return associatedLearner.isConnectedToClient(clientId);
   }

   @Override
   public void sendReply(int clientId, Message reply) {
      associatedLearner.sendReplyToClient(reply, clientId);
   }

   @Override
   public MulticastAgent getMulticastAgent() {
      return associatedMulticastAgent;
   }

}
