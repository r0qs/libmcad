package ch.usi.dslab.bezerra.mcad.recoverytester;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.mcad.MulticastClientServerFactory;


/** There are two groups and three rings. R1 -> G1, R2 -> G2, R3 -> G1,G1.
 * The <tt>MessageGenerator</tt> sends message to G1 and to G1,G2, therefore
 * passing by the rings R1 and R3. Members of group G1 would merge deliveries
 * from those rings. For each message to G1,G2, there are 1000 for G1, so
 * the number of skips will be pretty unbalanced.<br>
 * <br>
 * The learner will deliver a number <b>n</b> of messages (say, 10000), hash the
 * sequence into a hash <b>h1</b>, set the safest instance to trim as the same for
 * both rings. Then, it will do it again for the next <b>n</b> messages, creating
 * hash <b>h2</b>, and so on. The learner will, basically, output the hash of each
 * sequence of <b>n</b> messages. After that, the learner will crash, recover and
 * see if the recovered sequence also hashes to <b>h2</b>.
 * @author eduardo
 *
 */
public class MessageGenerator implements Runnable {
   
   MulticastClient mcclient;
   Thread generatorThread;
   int messageCount;
   Random rand;
   boolean running;
   
   public MessageGenerator(String configFile, int id) {
      rand = new Random(System.nanoTime());
      ClientMessage.setGlobalClientId(id);
      mcclient = MulticastClientServerFactory.getClient(id, configFile);
      // for (int contactServerId : contactServerIds)
      // mcclient.connectToServer(contactServerId);
      mcclient.connectToOneServerPerPartition();
      generatorThread = new Thread(this, "GeneratorThread");
   }

   void generateMessage() {
      int ratio = 100;
      
      List<Group> destinations = new ArrayList<Group>();
      
      messageCount++;
      if (messageCount % ratio != 0) {
         // send to G1
         destinations.add(Group.getGroup(1));
      }
      else {
         //send to all groups
         destinations.addAll(Group.getAllGroups());
//         destinations.add(Group.getGroup(1));
//         destinations.add(Group.getGroup(2));
      }
      
      ClientMessage msg = new ClientMessage(rand.nextInt());
      
      mcclient.multicast(destinations, msg);
   }
   
   public void startRunning() {
      running = true;
      generatorThread.start();
   }
   
   public void stopRunning() {
      running = false;
   };
   
   @Override
   public void run() {
      int burstLength = 10000;
      int count = 0;

      while (running) {
         if (++count % burstLength != 0) {
            generateMessage();
         }
         else {
//         if (count++ % burstLength == 0) {
//            try {
               System.out.println("Finished sending messages...");
//               Thread.sleep(10000);
//               System.out.println("Woke up! Sendin 10000 more...");
//            } catch (InterruptedException e) {
//               e.printStackTrace();
//            }
               break;
         }
      }
      
   }
   
   public static void main(String[] args) {
      String config = args[0];
      int id = Integer.parseInt(args[1]);
      MessageGenerator gen = new MessageGenerator(config, id);
      gen.startRunning();
   }

}
