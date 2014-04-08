package ch.usi.dslab.bezerra.mcad;

import java.util.List;

import ch.usi.dslab.bezerra.netwrapper.Message;

public interface MulticastAgent {
//   public void multicast(Group single_destination, byte [] message);
//   public void multicast(List<Group> destinations, byte [] message);
   public void multicast(Group single_destination, Message message);
   public void multicast(List<Group> destinations, Message message);
   
//   public byte [] deliver();
   public Message deliverMessage();
   
//   public boolean isDeserializingToMessage();
   
   public Group getLocalGroup();
}
