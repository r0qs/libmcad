package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.EventType;

public class URPRingWatcher implements Watcher {

   int ringId;
   List<Integer> learners;
   ZooKeeper zkClient;
   String ringLearnersPath;
   String ringPath;
   String zkAddress;
//   private Semaphore connectedSemaphore = new Semaphore(0);
   
   public URPRingWatcher(int rId, String zkHostAndPort) {
      try {
         ringId = rId;
         learners = new CopyOnWriteArrayList<Integer>();
         ringPath = "/ringpaxos/topology" + ringId;
         ringLearnersPath = ringPath + "/learners";
         zkAddress = zkHostAndPort;
         zkClient = new ZooKeeper(zkAddress, 3000, this);
      } catch (IOException ie) {
         ie.printStackTrace();
         System.exit(1);
      }
   }
   
   synchronized void processLearnersZnodes(List<String> learnersZnodes) {
      learners.clear();
      for (String learnerZ : learnersZnodes) {
         int learnerId = Integer.parseInt(learnerZ);
         if (learners.contains(learnerId))
            continue;
         System.out.println("Adding learner " + learnerId + " to ring " + ringId);
         learners.add(learnerId);
      }
   }
   
   synchronized List<Integer> getLearners() {
      return new ArrayList<Integer>(learners);
   }
   
   @Override
   public void process(WatchedEvent event) {
      try {
         if (event.getType() == Event.EventType.None) {
            // We are are being told that the state of the
            // connection has changed
            switch (event.getState()) {
               case SyncConnected:
                  System.out.println("ZOOKEER URPRINGWATCHER CONNECTED!");
                  List<String> children = zkClient.getChildren(ringLearnersPath, true);
                  processLearnersZnodes(children);
                  break;
               case Expired:
                  // handle disconnection
                  break;
               default:
                  // handle other cases
                  break;
            }
         }
         if(event.getType() == EventType.NodeChildrenChanged){
            if(event.getPath().startsWith(ringLearnersPath)){
               System.out.println("ZOOKEER URPRINGWATCHER :: learner ADDED!");
               List<String> l = zkClient.getChildren(ringLearnersPath, true);
               processLearnersZnodes(l);
            }
         }
      } catch (KeeperException ke) {
         System.err.println(":::: EXCEPTION being ignored:");
         ke.printStackTrace();
      } catch (InterruptedException ie) {
         ie.printStackTrace();
         System.exit(1);
      }     
   }

}
