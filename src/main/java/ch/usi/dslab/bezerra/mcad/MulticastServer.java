package ch.usi.dslab.bezerra.mcad;

import ch.usi.dslab.bezerra.netwrapper.Message;

public interface MulticastServer {
   void sendReply(int clientId, Message reply);
}
