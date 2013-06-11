package ch.usi.dslab.bezerra.mcad;

import java.util.ArrayList;

public class MCMessage {

   ArrayList<Group> destinations;
   byte[] payload;
   
   public ArrayList<Group> getDestinations() {
      return destinations;
   }
   
   public void setDestinations(ArrayList<Group> destinations) {
      this.destinations = destinations;
   }
   
   public byte[] getPayload() {
      return payload;
   }
   
   public void setPayload(byte[] payload) {
      this.payload = payload;
   }

}
