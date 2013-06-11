package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.util.ArrayList;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MCMessage;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;

public class URPMcastAgent implements MulticastAgent {

   @Override
   public void multicast(ArrayList<Group> destinations, byte [] message) {
      // TODO Auto-generated method stub
      
   }
   
   @Override
   public void multicast(Group single_destinations, byte [] message) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public byte [] deliver() {
      // TODO Auto-generated method stub
      return new byte[1];
   }
   
   // to translate from Group (.id) to whatever this implementation uses to represent a group
   // void addMapping(Group g, whatever urp uses inside to represent a group)
   
   // set up whatever configuration this specific mcast agent needs
   // void loadURPAgentConfig(String filename);

}
