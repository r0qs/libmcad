package ch.usi.dslab.bezerra.mcad.ridge;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import ch.usi.dslab.bezerra.mcad.ClientReceiver;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.Client;
import ch.usi.dslab.bezerra.ridge.RidgeMessage;


public class RidgeClientReceiver extends Client implements ClientReceiver {
   
   BlockingQueue<Message> receivedReplies;
   
   public RidgeClientReceiver(int id) {
      super(id);
      receivedReplies = new LinkedBlockingDeque<Message>();
   }
   
   @Override
   public void uponDelivery(RidgeMessage reply) {
      receivedReplies.add(reply);
   }

   @Override
   public Object deliver() {
      Object delivery = null;
      try {
         delivery = receivedReplies.take();
      } catch (InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return delivery;
   }

}
