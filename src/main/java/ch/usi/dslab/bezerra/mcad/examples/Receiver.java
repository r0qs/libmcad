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

import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastAgentFactory;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class Receiver extends Thread {
   
   MulticastAgent mcagent;
   
   public Receiver(int nodeId, int groupId, String configFile) {
      mcagent = MulticastAgentFactory.createMulticastAgent(configFile, true, groupId, nodeId);
   }   
   
   @Override
   public void run() {
      while (true) {
         Message msg = mcagent.deliverMessage();
         
         long now = System.currentTimeMillis();
         String text = (String) msg.getNext();
         long sendTime = (Long) msg.getNext();
         System.out.println(String.format("delivered message \"%s\" within %d ms", text, now - sendTime));
      }
   }

   public static void main(String[] args) {
      /*

       one of the receivers should start with parameters:  9 1
         the other receiver should start with parameters: 10 2

       The first parameter is the node id, and the second one is the group id to which the node belongs.
       Such node must be in the ridge configuration file, under the *group_members* section. This
       means that (for now) the whole system configuration is static, given in the config file.

      */
      
      int nodeId  = Integer.parseInt(args[0]);
      int groupId = Integer.parseInt(args[1]);
      String configFile = args[2];
      //String configFile = "/Users/eduardo/repositories/mine/libmcad/src/main/java/ch/usi/dslab/bezerra/mcad/examples/ridge_2g3e.json";
      
      Receiver receiver = new Receiver(nodeId, groupId, configFile);
      receiver.start();
   }

}
