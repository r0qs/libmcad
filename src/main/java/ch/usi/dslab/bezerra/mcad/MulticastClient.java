package ch.usi.dslab.bezerra.mcad;

import java.util.List;

import ch.usi.dslab.bezerra.netwrapper.Message;

public interface MulticastClient {
   void    connectToServer(int serverId);
   void    connectToOneServerPerPartition();
   Message deliverReply();
   void    multicast(List<Group> destinations, Message msg);
}
