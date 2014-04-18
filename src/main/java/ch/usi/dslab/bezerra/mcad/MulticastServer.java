package ch.usi.dslab.bezerra.mcad;

import ch.usi.dslab.bezerra.netwrapper.Message;

public interface MulticastServer {
   boolean isConnectedToClient(int clientId);
   
   void sendReply(int clientId, Message reply);
   
   MulticastAgent getMulticastAgent();
}
