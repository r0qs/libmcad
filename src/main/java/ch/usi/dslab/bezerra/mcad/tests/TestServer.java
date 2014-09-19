package ch.usi.dslab.bezerra.mcad.tests;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.usi.dslab.bezerra.mcad.FastMulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.mcad.MulticastServer;
import ch.usi.dslab.bezerra.mcad.OptimisticMulticastAgent;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.RidgeMessage.MessageIdentifier;

public class TestServer {
   
   public static class ListHashPrinter extends Thread {
      private String listName;
      private List<Object> objList;
      private String lastHash = "";
      
      @SuppressWarnings({ "unchecked", "rawtypes" })
      public ListHashPrinter(String listName, List objList) {
         super ("ListHashPrinter");
         this.listName = listName;
         this.objList  = objList;
      }
      
      public String byteArrayToHexString(byte[] b) {
         String result = "";
         for (int i=0; i < b.length; i++) {
           result +=
                 Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
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
      
      boolean compactList() {
         synchronized(objList) {
            final int trimSize = 100;
            if (objList.size() >= trimSize) {
               String sequence = lastHash;
               for (int i = 0 ; i < trimSize ; i++) {
                  sequence += String.format(" %s", objList.get(0));
                  objList.remove(0);
               }
               lastHash = toHash(sequence.getBytes());
               return false;
            }
            else
               return true;
         }
      }
      
      String getListHash() {
         String sequence, hash;
         synchronized (objList) {
            while(compactList() == false);
            sequence = lastHash;
            for (Object obj : objList)
               sequence += String.format(" %s", obj);
            hash = toHash(sequence.getBytes()).substring(0, 7);
         }
         return hash;
      }
      
      public void run() {
         long printInterval = 2500;
         while (true) {
            try {
               Thread.sleep(printInterval);
            } catch (InterruptedException e) {
               e.printStackTrace();
               System.exit(1);
            }
            System.out.println(String.format("hash of [%s]: %s", listName, getListHash()));
         }
      }
   }
   
   public static class ConservativeDeliverer extends Thread {
      MulticastServer mcServer;
      List<MessageIdentifier> allDeliveries = new ArrayList<MessageIdentifier>();
      ListHashPrinter         allDeliveriesHashPrinter = new ListHashPrinter("all", allDeliveries);
      Map<String, List<MessageIdentifier>> receivedMessages = new ConcurrentHashMap<String, List<MessageIdentifier>>();
      Map<String, ListHashPrinter> printers = new ConcurrentHashMap<String, ListHashPrinter>();
      
      public ConservativeDeliverer(TestServer parent) {
         super("ConservativeDeliverer");
         mcServer = parent.mcserver;
         allDeliveriesHashPrinter.start();
      }
      
      void addDelivery(String destsStr, MessageIdentifier mid) {
         List<MessageIdentifier> destMsgs = receivedMessages.get(destsStr);
         if (destMsgs == null) {
            destMsgs = new ArrayList<MessageIdentifier>();
            receivedMessages.put(destsStr, destMsgs);
            ListHashPrinter lhp = new ListHashPrinter(destsStr, destMsgs);
            lhp.start();
         }
         synchronized (destMsgs) {
            destMsgs.add(mid);
         }
         synchronized (allDeliveries) {
            allDeliveries.add(mid);
         }
      }
      
      public void run() {
         while (true) {
            Message msg = mcServer.getMulticastAgent().deliverMessage();
            long now = System.currentTimeMillis();
            
            int clientId = (Integer) msg.getNext();
            MessageIdentifier mid = (MessageIdentifier) msg.getNext();
            long timestamp = (Long) msg.getNext();
            String destinationString = (String) msg.getNext();
            
            addDelivery(destinationString, mid);
            
            Message reply = new Message(mid, DeliveryType.CONS);
            mcServer.sendReply(clientId, reply);

            System.out.println(String.format("cons-delivered message %s within %d ms", mid, now - timestamp));
         }
      }
   }
   
   public static class OptimisticDeliverer extends Thread {
      MulticastServer mcServer;
      public OptimisticDeliverer(TestServer parent) {
         super("OptimisticDeliverer");
         mcServer = parent.mcserver;
      }
      
      public void run() {
         while (true) {
            OptimisticMulticastAgent omcagent = (OptimisticMulticastAgent) mcServer.getMulticastAgent();
            
            Message msg = omcagent.deliverMessageOptimistically();
            
            long now = System.currentTimeMillis();
            
            int clientId = (Integer) msg.getNext();
            MessageIdentifier mid = (MessageIdentifier) msg.getNext();
            long timestamp = (Long) msg.getNext();
            
            Message reply = new Message(mid, DeliveryType.OPT);
            mcServer.sendReply(clientId, reply);

            System.out.println(String.format("opt-delivered message %s within %d ms", mid, now - timestamp));
         }
      }
   }
   
   public static class FastDeliverer extends Thread {
      MulticastServer mcServer;
      public FastDeliverer(TestServer parent) {
         super("FastDeliverer");
         mcServer = parent.mcserver;
      }
      
      public void run() {
         while (true) {
            FastMulticastAgent fmcagent = (FastMulticastAgent) mcServer.getMulticastAgent();
            
            Message msg = fmcagent.deliverMessageFast();
            
            long now = System.currentTimeMillis();
            
            int clientId = (Integer) msg.getNext();
            MessageIdentifier mid = (MessageIdentifier) msg.getNext();
            long timestamp = (Long) msg.getNext();
            
            Message reply = new Message(mid, DeliveryType.FAST);
            mcServer.sendReply(clientId, reply);

            System.out.println(String.format("fast-delivered message %s within %d ms", mid, now - timestamp));
         }
      }
   }   
   
   MulticastServer mcserver;
   ConservativeDeliverer consThread;
   OptimisticDeliverer    optThread;
   FastDeliverer         fastThread;
   
   public TestServer(int serverId, String configFile) {
      mcserver = MulticastClientServerFactory.getServer(serverId, configFile);
      consThread = new ConservativeDeliverer(this);
      optThread  = new OptimisticDeliverer(this);
      fastThread = new FastDeliverer(this);
   }
   
   public void start() {
      consThread.start();
      optThread.start();
      fastThread.start();
   }

   public static void main(String[] args) {
      /*

       one of the receivers should start with parameters:  9
         the other receiver should start with parameters: 10

       The parameter is the node id.
       Such node must be in the ridge configuration file, under the *group_members* section. This
       means that (for now) the whole system configuration is static, given in the config file.

      */
      
      int    nodeId     = Integer.parseInt(args[0]);
      String configFile = args[1];
      
      TestServer server = new TestServer(nodeId, configFile);
      server.start();
   }

}
