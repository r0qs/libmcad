package ch.usi.dslab.bezerra.mcad;

import java.util.concurrent.atomic.AtomicLong;

import ch.usi.dslab.bezerra.netwrapper.Message;

public class ClientMessage extends Message {
   private static final long serialVersionUID = 1L;
   private static AtomicLong nextSeq = new AtomicLong(0);
   private static int globalClientId = -1;

   public static void setGlobalClientId(int id) {
      globalClientId = id;
   }
   
   public static int getGlobalClientId() {
      return globalClientId;
   }
   
   int clientId;
   long msgSeq;
   
   public int getSourceClientId() {
      return clientId;
   }
   
   public void setSourceClientId(int id) {
      this.clientId = id;
   }

   public void setMessageSequence(long seq) {
      this.msgSeq = seq;
   }
   
   public long getMessageSequence() {
      return this.msgSeq;
   }
   
   public ClientMessage() {
      super();
      clientId = globalClientId;
      msgSeq   = nextSeq.incrementAndGet();
   }
   
   public ClientMessage(Object... objs) {
      super(objs);
      clientId = globalClientId;
      msgSeq   = nextSeq.incrementAndGet();
   }
   
}
