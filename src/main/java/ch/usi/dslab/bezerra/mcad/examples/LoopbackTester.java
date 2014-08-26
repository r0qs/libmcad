package ch.usi.dslab.bezerra.mcad.examples;

import java.util.ArrayList;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastAgentFactory;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class LoopbackTester {

   public static void main(String[] args) {
      String configFile = args[0];
      int nodeId  = Integer.parseInt(args[1]);
      int groupId = Integer.parseInt(args[2]);
      //final MulticastAgent mcagent = MulticastAgentFactory.createMulticastAgent(configFile, true, 0, 9);
      final MulticastAgent mcagent = MulticastAgentFactory.createMulticastAgent(configFile, true, groupId, nodeId);
      int i = 0;
      
      Thread deliverer = new Thread () {
         @Override
         public void run() {
            while (true) {
               Message msg = mcagent.deliverMessage();
               System.out.println("<--- delivered message  " + (String) msg.getItem(0));
            }
         }         
      };
      
      deliverer.start();
      
      int message_seq = 0;
      while (true) {
         try {
            Thread.sleep(100);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
         i++   ;
         i %= 3;
//         i = 1;
         String message;
         ArrayList<Group> dests;
         switch (i) {
            case 0 :
               message = "Message " + message_seq++ + " to group 1 only";
               System.out.println("---> Sending \""+ message + "\" (" + message.length() + " bytes)");
               mcagent.multicast(Group.getGroup(1), new Message(message));
               break;
               
            case 1 :
               message = "Message " + message_seq++ + " to group 2 only";
               System.out.println("---> Sending \""+ message + "\" (" + message.length() + " bytes)");
               mcagent.multicast(Group.getGroup(2), new Message(message));
               break;
               
//            case 1 :
            case 2 :
               message = "Message " + message_seq++ + " to group 1 and 2";
               System.out.println("---> Sending \""+ message + "\" (" + message.length() + " bytes)");
               dests = new ArrayList<Group>();
               dests.add(Group.getGroup(1));
               dests.add(Group.getGroup(2));
               mcagent.multicast(dests, new Message(message));               
               break;
               
            case 3 :
               message = "Message to group 3 only";
               System.out.println("---> Sending \""+ message + "\" (" + message.length() + " bytes)");
               dests = new ArrayList<Group>();
//               dests.add(Group.getGroup(1));
               dests.add(Group.getGroup(3));
               mcagent.multicast(dests, new Message(message));
               break;
               
            case 4 :
               message = "Message to group 2 and 3";
               System.out.println("---> Sending \""+ message + "\" (" + message.length() + " bytes)");
               dests = new ArrayList<Group>();
               dests.add(Group.getGroup(2));
               dests.add(Group.getGroup(3));
               mcagent.multicast(dests, new Message(message));
               break;
            default:
               break;            
         }
      }
   }
}
