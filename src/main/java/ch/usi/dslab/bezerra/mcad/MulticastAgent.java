package ch.usi.dslab.bezerra.mcad;

import java.util.ArrayList;

public interface MulticastAgent {
   public void multicast(Group single_destinations, byte [] message);
   public void multicast(ArrayList<Group> destinations, byte [] message);
   public byte [] deliver();
}
