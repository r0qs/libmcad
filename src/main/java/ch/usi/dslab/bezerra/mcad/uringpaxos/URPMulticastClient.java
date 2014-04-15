package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.util.List;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class URPMulticastClient implements MulticastClient {

   public URPMulticastClient(int clientId, String configFile) {
      
   }
   
   @Override
   public Message deliverReply() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void multicast(List<Group> destinations, Message msg) {
      // TODO Auto-generated method stub
      
   }

}
