package ch.usi.dslab.bezerra.mcad;

import java.util.List;

import ch.usi.dslab.bezerra.netwrapper.Message;

public interface MulticastClient {
   public Message deliverReply();
   public void    multicast(List<Group> destinations, Message msg);
}
