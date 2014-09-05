package ch.usi.dslab.bezerra.mcad.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastAgentFactory;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class Sender {
   
   MulticastAgent mcagent;
   BufferedReader br;
   
   public Sender (int senderId, String configFile) {
      int notUsed = 0;
      mcagent = MulticastAgentFactory.createMulticastAgent(configFile, false, notUsed, senderId);
   }
   
   public String askForInput() throws IOException {
      if (br == null) br = new BufferedReader(new InputStreamReader(System.in));
      System.out.println("multicast message m to g1, to g2 or to g1 & g2. (examples: \"MSG 1 2\", \"m 1\", \"Message 2\"):");
      String input = br.readLine();
      return input;
   }
   
   public void sendMessage(String text, List<Group> destinations) {
      Message multicastMessage = new Message(text, System.currentTimeMillis());
      mcagent.multicast(destinations, multicastMessage);
   }
   
   public static void main(String[] args) throws IOException {
      String configFile = args[0];
      int    senderId   = Integer.parseInt(args[1]);
      
      Sender sender = new Sender(senderId, configFile);
      
      String input = sender.askForInput();
      
      while (input.equalsIgnoreCase("end") == false) {
         String[] params = input.split(" ");
         
         String message = params[0];
         List<Group> destinationGroups = new ArrayList<Group>();
         for (int i = 1 ; i < params.length ; i++) {
            int groupId = Integer.parseInt(params[i]);
            destinationGroups.add(Group.getGroup(groupId));
         }
         sender.sendMessage(message, destinationGroups);
         
         input = sender.askForInput();
      }
   }

}
