package ch.usi.dslab.bezerra.mcad.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class TestClient {
   
   public static class ReplyDeliverer extends Thread {
      private TestClient parent;
      private Semaphore  wakeUpSignals = new Semaphore(0);;
      List<Long> pendingConsMessages = new ArrayList<Long>();
      List<Long> pendingOptMessages  = new ArrayList<Long>();
      List<Long> pendingFastMessages = new ArrayList<Long>();
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
      
      public void addPendingMessage(long seq) {
         addPendingMessage(seq, pendingConsMessages);
         addPendingMessage(seq, pendingOptMessages);
         addPendingMessage(seq, pendingFastMessages);
      }
      
      private void addPendingMessage(long seq, List<Long> pendingList) {
         synchronized (pendingList) {
            pendingList.add(seq);         
         }
         wakeUp();
      }
      
      private void removePendingMessage(long id, List<Long> pendingList) {
         boolean contained;
         synchronized (pendingList) {
            contained = pendingList.remove(id);
         }
         if (parent.printPending) {
            String listName = pendingList == pendingConsMessages ? "cons-pending" : (pendingList == pendingOptMessages ? "opt-pending" : "fast-pending");
            System.out.println(String.format("Message %s" + (contained ? " " : " already ") + "removed from " + listName + " (pending - c: %d, o: %d, f: %d)", id, pendingConsMessages.size(), pendingOptMessages.size(), pendingFastMessages.size()));
         }
      }
      
      private boolean containsPendingMessage(long id, List<Long> pendingList) {
         synchronized (pendingList) {
            return pendingList.contains(id);
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
            long mseq = (Long) reply.getNext();
            int deliveryType = (Integer) reply.getNext();
            switch (deliveryType) {
               case DeliveryType.CONS : {
                  removePendingMessage(mseq, pendingConsMessages);
                  if (containsPendingMessage(mseq, pendingConsMessages) == false) {
                     addPermit();
                  }
                  break;
               }
               case DeliveryType.OPT : {
                  removePendingMessage(mseq, pendingOptMessages);
                  break;
               }
               case DeliveryType.FAST : {
                  removePendingMessage(mseq, pendingFastMessages);
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
   
   public TestClient (int clientId, String configFile) {
      this.clientId = clientId;
      ClientMessage.setGlobalClientId(clientId);
      mcclient = MulticastClientServerFactory.getClient(clientId, configFile);
//      for (int contactServerId : contactServerIds)
//         mcclient.connectToServer(contactServerId);
      mcclient.connectToOneServerPerPartition();
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
      String destinationString = "";
      for (int i = 0 ; i < destinations.size() ; i++)
         destinationString += String.format(" %d", destinations.get(i).getId());
      ClientMessage clientMessage = new ClientMessage(System.currentTimeMillis(), destinationString);
      for (int i = 0 ; i < destinations.size() ; i++)
         verifier.addPendingMessage(clientMessage.getMessageSequence());
      
      
      // DEBUG
//      multicastMessage.t_client_send = System.currentTimeMillis();
      //======
      
      mcclient.multicast(destinations, clientMessage);
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
//      List<Integer> contactServers = new ArrayList<Integer>();
//      for (int i = 2 ; i < args.length ; i++)
//         contactServers.add(Integer.parseInt(args[i]));
      
//      TestClient client = new TestClient(clientId, contactServers, configFile);
      TestClient client = new TestClient(clientId, configFile);
      
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
            System.out.println(String.format("sending burst - length: %d, random: %b, to g1: %b, to g2: %b",
                  burstLength, random, sendtog1, sendtog2));
            client.sendBurst(burstLength, random, sendtog1, sendtog2);
         }
         else if (input.contains("l")) {
            boolean random = (input.contains("r"));
            boolean sendtog1, sendtog2;
            sendtog1 = sendtog2 = false;
            for (int i = 1 ; i < params.length - 1 ; i++) {
               if (isInt(params[i]) && getInt(params[i]) == 1) sendtog1 = true;
               if (isInt(params[i]) && getInt(params[i]) == 2) sendtog2 = true;
            }
            int outstanding = Integer.parseInt(params[params.length - 1]);
            System.out.println(String.format("sending closed loop - outstanding: %d, random: %b, to g1: %b, to g2: %b",
                  outstanding, random, sendtog1, sendtog2));
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
