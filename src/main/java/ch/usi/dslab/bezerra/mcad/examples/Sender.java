/*

 Libmcad - A multicast adaptor library
 Copyright (C) 2015, University of Lugano
 
 This file is part of Libmcad.
 
 Libmcad is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libmcad is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 
*/

/**
 * @author Eduardo Bezerra - eduardo.bezerra@usi.ch
 */

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
   
   public void sendBurst() {
      int numMessages = 100;
      for (int i = 0 ; i < numMessages ; i++) {
         String text = String.format("msg_%d", i);
         List<Group> destinations = new ArrayList<Group>();
         destinations.add(Group.getGroup(1));
         destinations.add(Group.getGroup(2));
         sendMessage(text, destinations);
      }
   }
   
   public static void main(String[] args) throws IOException {
      String configFile = args[0];
      int    senderId   = Integer.parseInt(args[1]);
      
      Sender sender = new Sender(senderId, configFile);
      
      String input = sender.askForInput();
      
      while (input.equalsIgnoreCase("end") == false) {

         if (input.equalsIgnoreCase("burst"))
            sender.sendBurst();
         else {
            String[] params = input.split(" ");
            String message = params[0];
            List<Group> destinationGroups = new ArrayList<Group>();
            for (int i = 1 ; i < params.length ; i++) {
               int groupId = Integer.parseInt(params[i]);
               destinationGroups.add(Group.getGroup(groupId));
            }
            sender.sendMessage(message, destinationGroups);
         }
         
         input = sender.askForInput();
      }
   }

}
