package ch.usi.dslab.bezerra.mcad;

import ch.usi.dslab.bezerra.netwrapper.Message;

public interface OptimisticMulticastAgent {
   public Message deliverMessageOptimistically();
}
