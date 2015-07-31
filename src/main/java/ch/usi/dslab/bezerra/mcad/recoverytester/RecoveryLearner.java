package ch.usi.dslab.bezerra.mcad.recoverytester;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.DeliveryMetadata;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.mcad.MulticastServer;

public class RecoveryLearner implements Runnable {

   public static class Hasher {

      int burstcounter    = 1;
      List<Integer> currentBurst;
      MulticastAgent mcagent;
      
      public Hasher(MulticastAgent mca) {
         mcagent = mca;
         currentBurst = new ArrayList<Integer>();
      }
      
      public void putInteger(int count, int i, boolean burstHead, DeliveryMetadata dm) {
         if (burstHead && currentBurst.size() > 0) {
            System.out.println(String.format("hash(%d deliveries) = %s", currentBurst.size(), hashAndClear(currentBurst)));
         }
         currentBurst.add(i);
         if (count == 500)
            mcagent.notifyCheckpointMade(dm);
      }
      
      private String hashAndClear(List<Integer> burst) {
         StringBuffer concat = new StringBuffer();
         
         for (int i : burst)
            concat.append(" " + i);
         burst.clear();
         String hash = toHash(concat.toString().getBytes()).substring(0, 7);
         return hash;
      }
      
      public String byteArrayToHexString(byte[] b) {
         String result = "";
         for (int i=0; i < b.length; i++) {
           result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
         }
         return result;
       }
      
      public String toHash(byte[] hashMe) {
         MessageDigest md = null;
         try {
             md = MessageDigest.getInstance("SHA-1");
         }
         catch(NoSuchAlgorithmException e) {
             e.printStackTrace();
         } 
         return byteArrayToHexString(md.digest(hashMe));
      }
      
   }
   
   MulticastServer mcServer;
   Thread learnerThread;
   boolean running;
   Hasher hasher;

   public RecoveryLearner(String configFile, int learnerId) {
      mcServer = MulticastClientServerFactory.getServer(learnerId, configFile);
      hasher = new Hasher(mcServer.getMulticastAgent());
      learnerThread = new Thread(this, "RecoveryLearner");
   }
   
   public void startRunning() {
      running = true;
      learnerThread.start();
   }
   
   @Override
   public void run() {
      boolean gotBurstHead = false;
      int count = 0;
      while (running) {
         ClientMessage msg = mcServer.deliverClientMessage();
         msg.rewind();
         int     value     = (Integer) msg.getNext();
         boolean burstHead = (Boolean) msg.getNext();
         
         if (burstHead)
            gotBurstHead = true;
         
         if (gotBurstHead)
            hasher.putInteger(count++, value, burstHead, (DeliveryMetadata) msg.getAttachment());                  
      }
   }
   
   public static void main(String[] args) {
      String config = args[0];
      int    id     = Integer.parseInt(args[1]);
      
      RecoveryLearner rl = new RecoveryLearner(config, id);
      rl.startRunning();
   }
   
}
