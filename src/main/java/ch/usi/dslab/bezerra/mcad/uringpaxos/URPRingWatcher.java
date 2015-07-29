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
   private boolean learnersReceived = false;
   private Semaphore learnersReceivedSemaphore = new Semaphore(0);
   
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
      if (!learnersReceived) {
         learnersReceived = true;
         learnersReceivedSemaphore.release();
      }      
   }
   
   synchronized List<Integer> getLearners() {
      return new ArrayList<Integer>(learners);
   }
   
   public void waitForInitialLearnersInfo() {
      if (!learnersReceived)
         learnersReceivedSemaphore.acquireUninterruptibly();
   }
   
   @Override
   public void process(WatchedEvent event) {
      try {
         if (event.getType() == Event.EventType.None) {
            // We are are being told that the state of the
            // connection has changed
            switch (event.getState()) {
               case SyncConnected:
                  System.out.println("ZooKeeper URPRingWatcher connected!");
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
               System.out.println("ZooKeeper URPRingWatcher :: learner added/removed!");
               List<String> l = zkClient.getChildren(ringLearnersPath, true);
               processLearnersZnodes(l);
            }
         }
      } catch (KeeperException ke) {
         System.err.println(":::: exception being ignored:");
         ke.printStackTrace();
      } catch (InterruptedException ie) {
         ie.printStackTrace();
         System.exit(1);
      }     
   }

}
