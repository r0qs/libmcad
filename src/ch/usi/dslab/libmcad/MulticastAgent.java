package ch.usi.dslab.libmcad;

import java.util.ArrayList;

public interface MulticastAgent {
   public void multicast(ArrayList<Group> destinations, Message message);
   public void deliver();
}
