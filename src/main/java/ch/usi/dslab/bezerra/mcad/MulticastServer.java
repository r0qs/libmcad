package ch.usi.dslab.bezerra.mcad;

import ch.usi.dslab.bezerra.netwrapper.Message;

public interface MulticastServer {
   boolean isConnected(int clientId);
   void    sendReply(int clientId, Message reply);
}
