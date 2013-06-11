package ch.usi.dslab.bezerra.libmcad;

import java.util.ArrayList;

public interface MulticastAgent {
   public void multicast(Group single_destinations, MCMessage message);
   public void multicast(ArrayList<Group> destinations, MCMessage message);
   public MCMessage deliver();
}
