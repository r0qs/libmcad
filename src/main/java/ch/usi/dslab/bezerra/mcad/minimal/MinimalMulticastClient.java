package ch.usi.dslab.bezerra.mcad.minimal;

import java.util.List;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class MinimalMulticastClient implements MulticastClient {

   public MinimalMulticastClient(int clientId, String configFile) {
      
   }
   
   @Override
   public void connectToServer(int serverId) {
      // TODO Auto-generated method stub
   }
   
   @Override
   public Message deliverReply() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void multicast(List<Group> destinations, Message message) {
      // TODO Auto-generated method stub
   }

}
