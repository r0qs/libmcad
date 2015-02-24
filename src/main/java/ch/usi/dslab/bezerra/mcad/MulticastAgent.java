package ch.usi.dslab.bezerra.mcad;

import java.util.List;

import ch.usi.dslab.bezerra.netwrapper.Message;

public interface MulticastAgent {
   public void multicast(Group single_destination, Message message);
   public void multicast(List<Group> destinations, Message message);
   public Message deliverMessage();
   public Group getLocalGroup();
}
