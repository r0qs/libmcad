package ch.usi.dslab.bezerra.mcad.recoverytester;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.DeliveryMetadata;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastCheckpoint;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.mcad.MulticastServer;

public class RecoveryLearner implements Runnable {
   
   public static class Hasher {

      int burstcounter    = 1;
      List<Integer> currentBurst;
      MulticastAgent mcagent;
      MulticastServer mcserver;
      DeliveryMetadata lastDeliveryMetadata = null;
      int cpsMade = 0;
      int cpsMax  = 4;
      int totalDelivered = 0;
      
      public Hasher(MulticastServer mcs) {
         mcserver = mcs;
         mcagent = mcs.getMulticastAgent();
         currentBurst = new ArrayList<Integer>();
      }
      
      public void putInteger(int i, boolean burstHead, DeliveryMetadata dm) {
         if (burstHead && currentBurst.size() > 0) {
            System.out.println(String.format("hash(%d deliveries) = %s", currentBurst.size(), hashAndClear(currentBurst)));
            if (cpsMade++ < cpsMax)
               createCheckpoint(lastDeliveryMetadata);
         }
         currentBurst.add(i);
         lastDeliveryMetadata = dm;
      }
      
      private void createCheckpoint(DeliveryMetadata dm) {
         MulticastCheckpoint mcp = mcagent.createMulticastCheckpoint(dm);
         
         String checkpointFileName = "/tmp/urpmcastcheckpoint_" + mcserver.getId() + ".bin";
         
         try {
            ObjectOutputStream oos = new ObjectOutputStream(new DeflaterOutputStream(new FileOutputStream(checkpointFileName)));
            oos.writeObject(mcp);
            oos.close();
         }
         catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }
         
         mcagent.notifyCheckpointMade(dm);
      }
      
      private MulticastCheckpoint loadMulticastCheckpoint() {
         String checkpointFileName = "/tmp/urpmcastcheckpoint_" + mcserver.getId() + ".bin";
         
         File f = new File(checkpointFileName);
         if(!f.exists())
            return null;
         
         System.out.println("Located checkpoint " + checkpointFileName + ". Loading it...");
         
         try {
            ObjectInputStream ois = new ObjectInputStream(new InflaterInputStream(new FileInputStream(checkpointFileName)));
            MulticastCheckpoint cp = (MulticastCheckpoint) ois.readObject();
            ois.close();
            return cp;
         }
         catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
         }
         
         return null;
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
      hasher = new Hasher(mcServer);
      learnerThread = new Thread(this, "RecoveryLearner");
   }
   
   public void startRunning() {
      running = true;
      learnerThread.start();
   }
   
   @Override
   public void run() {
      
      MulticastCheckpoint mcp = hasher.loadMulticastCheckpoint();
      mcServer.getMulticastAgent().provideMulticastCheckpoint(mcp);
      
      while (running) {
         ClientMessage msg = mcServer.deliverClientMessage();
         msg.rewind();
         int     value     = (Integer) msg.getNext();
         boolean burstHead = (Boolean) msg.getNext();
         
         hasher.putInteger(value, burstHead, (DeliveryMetadata) msg.getAttachment());                  
      }
   }
   
   public static void main(String[] args) {
      String config = args[0];
      int    id     = Integer.parseInt(args[1]);
      
      RecoveryLearner rl = new RecoveryLearner(config, id);
      rl.startRunning();
   }
   
}
