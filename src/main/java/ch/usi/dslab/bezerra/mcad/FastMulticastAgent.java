package ch.usi.dslab.bezerra.mcad;

import ch.usi.dslab.bezerra.netwrapper.Message;

public interface FastMulticastAgent extends MulticastAgent {
   public Message deliverMessageFast();
}
