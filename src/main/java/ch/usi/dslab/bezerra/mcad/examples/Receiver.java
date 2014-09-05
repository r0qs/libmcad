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
