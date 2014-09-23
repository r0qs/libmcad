package ch.usi.dslab.bezerra.mcad.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.RidgeMessage.MessageIdentifier;

public class TestClient {
   
   public static class ReplyDeliverer extends Thread {
      private TestClient parent;
      private Semaphore  wakeUpSignals = new Semaphore(0);;
      List<MessageIdentifier> pendingConsMessages = new ArrayList<MessageIdentifier>();
      List<MessageIdentifier> pendingOptMessages  = new ArrayList<MessageIdentifier>();
      List<MessageIdentifier> pendingFastMessages = new ArrayList<MessageIdentifier>();
      private Semaphore outstandingPermits;
      
      public ReplyDeliverer(TestClient parent) {
         super("ReplyDeliverer");
         this.parent = parent;
      }
      
      public void setOutstanding(int num) {
         outstandingPermits = new Semaphore(num);
      }
      
      public void getPermit() {
         try {
            if (outstandingPermits != null) outstandingPermits.acquire();
         } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
         }
      }
      
      private void addPermit() {
         if (outstandingPermits != null) outstandingPermits.release();
      }
      
      public void addPendingMessage(MessageIdentifier id) {
         addPendingMessage(id, pendingConsMessages);
         addPendingMessage(id, pendingOptMessages);
         addPendingMessage(id, pendingFastMessages);
      }
      
      private void addPendingMessage(MessageIdentifier id, List<MessageIdentifier> pendingList) {
         synchronized (pendingList) {
            pendingList.add(id);         
         }
         wakeUp();
      }
      
      private void removePendingMessage(MessageIdentifier id, List<MessageIdentifier> pendingList) {
         boolean contained;
         synchronized (pendingList) {
            contained = pendingList.remove(id);
         }
         if (parent.printPending) {
            String listName = pendingList == pendingConsMessages ? "cons-pending" : (pendingList == pendingOptMessages ? "opt-pending" : "fast-pending");
            System.out.println(String.format("Message %s" + (contained ? " " : " already ") + "removed from " + listName + " (pending - c: %d, o: %d, f: %d)", id, pendingConsMessages.size(), pendingOptMessages.size(), pendingFastMessages.size()));
         }
      }
      
      private void wakeUp() {
         wakeUpSignals.release();
      }
      
      private void sleep() {
         try {
            wakeUpSignals.acquire();
         } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
         }
      }
      
      public void run() {
         while (true) {
            if (pendingConsMessages.isEmpty() && pendingOptMessages.isEmpty() && pendingFastMessages.isEmpty())
               sleep();
            Message reply = parent.mcclient.deliverReply();
            MessageIdentifier mid = (MessageIdentifier) reply.getNext();
            int deliveryType = (Integer) reply.getNext();
            switch (deliveryType) {
               case DeliveryType.CONS : {
                  addPermit();
                  removePendingMessage(mid, pendingConsMessages);
                  break;
               }
               case DeliveryType.OPT : {
                  removePendingMessage(mid, pendingOptMessages);
                  break;
               }
               case DeliveryType.FAST : {
                  removePendingMessage(mid, pendingFastMessages);
                  break;
               }
               default : {
                  break;
               }
            }
         }
      }
   }
   
   int clientId;
   MulticastClient mcclient;
   BufferedReader br;
   ReplyDeliverer verifier;
   boolean printPending = true;
   
   public TestClient (int clientId, List<Integer> contactServerIds, String configFile) {
      this.clientId = clientId;
      mcclient = MulticastClientServerFactory.getClient(clientId, configFile);
      for (int contactServerId : contactServerIds)
         mcclient.connectToServer(contactServerId);
      verifier = new ReplyDeliverer(this);
      verifier.start();
   }
   
   public String askForInput() throws IOException {
      if (br == null) br = new BufferedReader(new InputStreamReader(System.in));
      System.out.println("example inputs:\n1\n1 2\n2\nb 1 100\nb 2 200\nb 1 2 50\nb r 1 2 1000");
      String input = br.readLine();
      return input;
   }
   
   
   
   public void sendMessage(List<Group> destinations) {
      MessageIdentifier mid = MessageIdentifier.getNextMessageId(clientId);
      String destinationString = "";
      for (int i = 0 ; i < destinations.size() ; i++) {
         destinationString += String.format(" %d", destinations.get(i).getId());
         for (int j = 0 ; j < destinations.get(i).getMembers().size() ; j++)
            verifier.addPendingMessage(mid);            
      }
      Message multicastMessage = new Message(clientId, mid, System.currentTimeMillis(), destinationString);
      mcclient.multicast(destinations, multicastMessage);
   }
   
   public void sendBurst(int numMessages, boolean random, boolean g1, boolean g2) {
      Random rand = new Random(System.nanoTime());
      for (int i = 0 ; i < numMessages ; i++) {
         List<Group> destinations = new ArrayList<Group>();
         if (g1 && (!random || (random && rand.nextInt(2) == 1)))
            destinations.add(Group.getGroup(1));
         if (g2 && (!random || (random && rand.nextInt(2) == 1)))
            destinations.add(Group.getGroup(2));
         if (destinations.size() > 0)
            sendMessage(destinations);
      }
   }
   
   public void sendClosedLoop(int outstanding, boolean random, boolean sendtog1, boolean sendtog2) {
      if (!sendtog1 && !sendtog2) {
         System.out.println("Must send to at least one group");
         return;
      }
      printPending = false;
      verifier.setOutstanding(outstanding);
      Random rand = new Random(System.nanoTime());
      while (true) {
         List<Group> destinations = new ArrayList<Group>();
         if (sendtog1 && (!random || (random && rand.nextInt(2) == 1)))
            destinations.add(Group.getGroup(1));
         if (sendtog2 && (!random || (random && rand.nextInt(2) == 1)))
            destinations.add(Group.getGroup(2));
         if (destinations.size() > 0) {
            verifier.getPermit();
            sendMessage(destinations);
         }
      }
   }
   
   public static boolean isInt(String s) {
      try { 
         Integer.parseInt(s); 
     } catch(NumberFormatException e) { 
         return false; 
     }
     return true;
   }
   
   public static int getInt(String s) {
      try {
         return Integer.parseInt(s);
      } catch(NumberFormatException e) {
         return -1;
      }
   }
   
   public static void main(String[] args) throws IOException {
      
      /*
       
       To start the client, pass parameters configFile clientId contactServer1 [contactServer2 [...] ]
        
       */
      
      String configFile = args[0];
      int    clientId   = Integer.parseInt(args[1]);
      List<Integer> contactServers = new ArrayList<Integer>();
      for (int i = 2 ; i < args.length ; i++)
         contactServers.add(Integer.parseInt(args[i]));
      
      TestClient client = new TestClient(clientId, contactServers, configFile);
      
      String input = client.askForInput();
      
      while (input.equalsIgnoreCase("end") == false) {
         String[] params = input.split(" ");
         if (input.contains("b")) {
            boolean random = (input.contains("r"));
            boolean sendtog1, sendtog2;
            sendtog1 = sendtog2 = false;
            for (int i = 1 ; i < params.length - 1 ; i++) {
               if (isInt(params[i]) && getInt(params[i]) == 1) sendtog1 = true;
               if (isInt(params[i]) && getInt(params[i]) == 2) sendtog2 = true;
            }
            int burstLength = Integer.parseInt(params[params.length - 1]);
            client.sendBurst(burstLength, random, sendtog1, sendtog2);
         }
         if (input.contains("l")) {
            boolean random = (input.contains("r"));
            boolean sendtog1, sendtog2;
            sendtog1 = sendtog2 = false;
            for (int i = 1 ; i < params.length - 1 ; i++) {
               if (isInt(params[i]) && getInt(params[i]) == 1) sendtog1 = true;
               if (isInt(params[i]) && getInt(params[i]) == 2) sendtog2 = true;
            }
            int outstanding = Integer.parseInt(params[params.length - 1]);
            client.sendClosedLoop(outstanding, random, sendtog1, sendtog2);
         }
         else {
            List<Group> destinationGroups = new ArrayList<Group>();
            for (int i = 0 ; i < params.length ; i++) {
               int groupId = Integer.parseInt(params[i]);
               destinationGroups.add(Group.getGroup(groupId));
            }
            client.sendMessage(destinationGroups);
         }
         
         input = client.askForInput();
      }
   }

}
