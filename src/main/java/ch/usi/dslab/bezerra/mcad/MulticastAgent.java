package ch.usi.dslab.bezerra.mcad;

import java.util.List;

public interface MulticastAgent {
   public void multicast(Group single_destination, byte [] message);
   public void multicast(List<Group> destinations, byte [] message);
   public byte [] deliver();
   public Message deliverMessage();
   
   public boolean isDeserializingToMessage();
   
   public void rmcast(List<Group> destinations, byte [] message);

   public void rmcast(Group destination, byte [] message);
   
   public Group getLocalGroup();
}
