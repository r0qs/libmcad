package ch.usi.dslab.libmcad.uringpaxos;

import java.util.ArrayList;

import ch.usi.dslab.libmcad.Group;
import ch.usi.dslab.libmcad.Message;
import ch.usi.dslab.libmcad.MulticastAgent;

public class URPMcastAgent implements MulticastAgent {

   @Override
   public void multicast(ArrayList<Group> destinations, Message message) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void deliver() {
      // TODO Auto-generated method stub
      
   }
   
   // to translate from Group (.id) to whatever this implementation uses to represent a group
   // void addMapping(Group g, whatever urp uses inside to represent a group)
   
   // set up whatever configuration this specific mcast agent needs
   // void loadURPAgentConfig(String filename);

}
