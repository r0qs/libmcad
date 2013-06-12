package ch.usi.dslab.bezerra.mcad.minimal;

import java.util.ArrayList;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;

public class MinimalMcastAgent implements MulticastAgent {
   
   public MinimalMcastAgent (String configFile) {
      loadMinimalAgentConfig(configFile);
   }

   @Override
   public void multicast(Group single_destinations, byte[] message) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void multicast(ArrayList<Group> destinations, byte[] message) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public byte[] deliver() {
      // TODO Auto-generated method stub
      return null;
   }
   
   // to translate from Group (.id) to whatever this implementation uses to represent a group
   // void addMapping(Group g, whatever urp uses inside to represent a group)
   
   // set up whatever configuration this specific mcast agent needs
   public void loadMinimalAgentConfig(String filename) {
      
   }

}
