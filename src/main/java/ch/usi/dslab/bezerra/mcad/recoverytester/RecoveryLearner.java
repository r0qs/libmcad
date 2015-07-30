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

      final int burstSize = 1000;
      int burstcounter    = 1;
      List<Integer> currentBurst;
      MulticastAgent mcagent;
      
      public Hasher(MulticastAgent mca) {
         mcagent = mca;
         currentBurst = new ArrayList<Integer>();
      }
      
      public void putInteger(int i, DeliveryMetadata dm) {
         currentBurst.add(i);
         if (currentBurst.size() == burstSize) {
            System.out.println(String.format("hash_%d(%d deliveries) = %s", burstcounter++, burstSize, hashAndClear()));
            mcagent.notifyCheckpointMade(dm);
         }
//         if (burstcounter == 30)
//            System.exit(0);
      }
      
      private String hashAndClear() {
         StringBuffer concat = new StringBuffer();
         
         for (int i : currentBurst)
            concat.append(" " + i);
         currentBurst.clear();
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
      boolean discardedFirst = false;
      while (running) {
         ClientMessage msg = mcServer.deliverClientMessage();
         msg.rewind();
         int     value        = (Integer) msg.getNext();
         
         if (discardedFirst == false) {
            discardedFirst = true;
            continue;
         }
         
         hasher.putInteger(value, (DeliveryMetadata) msg.getAttachment());                  
      }
   }
   
   public static void main(String[] args) {
      String config = args[0];
      int    id     = Integer.parseInt(args[1]);
      
      RecoveryLearner rl = new RecoveryLearner(config, id);
      rl.startRunning();
   }
   
}
