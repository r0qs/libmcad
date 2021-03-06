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

import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ch.usi.da.paxos.api.Learner;
import ch.usi.da.paxos.api.PaxosRole;
import ch.usi.da.paxos.ring.Node;
import ch.usi.da.paxos.ring.RingDescription;
import ch.usi.dslab.bezerra.mcad.DeliveryMetadata;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.Util;
import ch.usi.dslab.bezerra.mcad.uringpaxos.URPMulticastServer.URPMcastServerInfo;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class URPMcastAgent implements MulticastAgent {
   public static final Logger log = Logger.getLogger(URPMcastAgent.class);
   
   private static URPMcastAgent instance = null;
   
   public static URPMcastAgent getAgent() {
      return instance;
   }
   
   int multiRingMergeBlock = 1;
   Node URPaxosNode = null;
   URPGroup localGroup = null;
   URPAgentLearner urpAgentLearner;
   URPDeliveryMetadata firstDeliveryMetadata;
   URPMulticastServer urpMcastServer;
   Map<Long, URPRingData> mappingGroupsToRings;
   BlockingQueue<byte[]> byteArrayDeliveryQueue;
   BlockingQueue<Message> messageDeliveryQueue;
   
   boolean deserializeToMessage = true;
   
   Multicaster multicaster;
   
   public class Multicaster implements Runnable {
      BlockingQueue<byte[]>      messagesToMulticast  = new LinkedBlockingQueue<byte[]>();
      BlockingQueue<List<Group>> messagesDestinations = new LinkedBlockingQueue<List<Group>>();
      
      Thread multicasterThread;
      
      boolean running = true;
      
      URPMcastAgent mcagent;
      
      public Multicaster(URPMcastAgent mcagent) {
         this.mcagent = mcagent;
         multicasterThread = new Thread(this);
         multicasterThread.start();
      }
      
      public void enqueueForMulticasting(List<Group> destinations, byte[] message) {
         messagesToMulticast.add(message);
         messagesDestinations.add(destinations);
      }
      
      @Override
      public void run() {
         try {
            while (running) {
               byte[] message = messagesToMulticast.poll(1, TimeUnit.SECONDS);
               if (message == null) continue;
               List<Group> dests = messagesDestinations.take();               
               mcagent.actuallyMulticast(dests, message);
            }
         }
         catch(InterruptedException e) {
            
         }
      }
   }
   
   /** Creates a multicast agent that is not part of any group. For instance, a
    *  client (which is never a multicast destination) would use this groupless
    *  multicast agent.
    * 
    * @param configFile - the configuration file that describes the multicast
    *                     deployment used by the agent.
    */
   public URPMcastAgent (String configFile) {
      this(configFile, false, -1);
   }
   
   public URPMcastAgent (String configFile, boolean isInGroup, int nodeId) {
      log.setLevel(Level.OFF);
      byteArrayDeliveryQueue = new LinkedBlockingQueue<byte[]> ();
      messageDeliveryQueue   = new LinkedBlockingQueue<Message>();
      loadURPAgentConfig(configFile, isInGroup, nodeId);
      firstDeliveryMetadata = null;
      multicaster = new Multicaster(this);
      instance = this;
   }
   
   /** This method returns the parameter M of Multi-Ring Paxos, i.e., how many instances
    *  from each ring are taken at once to do the deterministic merging.
    * 
    * @return The parameter M of Multi-Ring Paxos
    */
   public int getMergeBlockSize() {
      return multiRingMergeBlock;      
   }

   
   /** This method sets the parameter M of Multi-Ring Paxos, i.e., how many instances
    *  from each ring will be taken at once to do the deterministic merging.
    *  
    * @param M - the parameter M of Multi-Ring Paxos
    */
   public void setMergeBlockSize(int M) {
      this.multiRingMergeBlock = M;
   }
   
   void mapGroupsToRings() {
      mappingGroupsToRings = new Hashtable<Long, URPRingData>();
      
      List<Group> groups = Group.getAllGroups();
      
      // sort the groups list by group_id
      Collections.sort(groups, new Comparator<Group> () {
         @Override
         public int compare(Group g1, Group g2) {
           return g1.getId() - g2.getId();
         }
      });
      //=================================
      
      log.info("about to call recursive mapping with groups in " + groups);
      recursivelyMapAllGroupCombinations(groups, new ArrayList<Group>(), -1, false, 0);

   }
   
   void recursivelyMapAllGroupCombinations (List<Group> all, ArrayList<Group> destsPrevious, int curId, boolean curGroupIsPresent, long hash) {
      if (all.isEmpty()) return;
      
      ArrayList<Group> destinations = new ArrayList<Group>(destsPrevious);
      
      Group curGroup = Group.getGroup(curId);
      
      if (curGroupIsPresent && curGroup != null) {
         destinations.add(curGroup);
         hash += (long) Math.pow(2, curId);
      }
      
      if (curGroup == all.get(all.size() - 1)) {
         // if the destinations list is empty, or if some other path in the
         // tree already let to the same combination (i.e., if there is a gap
         // in the group id sequence), there is no point in executing this.
         if (destinations.isEmpty() || mappingGroupsToRings.containsKey(hash)) return;
         
         // iterate through all rings that deliver to all those groups, leaving only those that work
         // sort them by number of associated groups
         // return the first one         
         ArrayList<URPRingData> candidates = new ArrayList<URPRingData>(URPRingData.ringsList);
         
         // getting a valid list of candidate rings
         next_candidate:
         for (URPRingData r : URPRingData.ringsList){            
            for (Group g : destinations) {
               if (r.destinationGroups.contains(g) == false) {
                  candidates.remove(r);
                  continue next_candidate;
               }
            }
         }
         
         // sorting the candidate rings by number of associated groups
         Collections.sort(candidates, new Comparator<URPRingData> () {
            @Override
            public int compare(URPRingData r1, URPRingData r2) {
               return r1.destinationGroups.size() - r2.destinationGroups.size();
            }            
         });
         
         // getting the best candidate ring (i.e., that with the least number of associated groups)
         // and indexing it for this set of destinations
         URPRingData bestCandidateRing = candidates.isEmpty() ? null : candidates.get(0);
         mappingGroupsToRings.put(hash, bestCandidateRing);
         
         log.info("Added mapping of destination set " + destinations +
               " (with hash " + hash + ") to ring "
               + (bestCandidateRing == null ? "null" : bestCandidateRing.getId()));
         
      }
      else {
         // hash became : hash + 2 ^ curId
         recursivelyMapAllGroupCombinations(all, destinations, curId + 1, true , hash);
         // hash unchanged
         recursivelyMapAllGroupCombinations(all, destinations, curId + 1, false, hash);
      }
   }
   
   long hashDestinationSet(List<Group> givenDestinations) {
      long hash = 0;
      
      // removing possible duplicates in the the destinations list:
      List<Group> destinations = new ArrayList<Group>(new HashSet<Group>(givenDestinations));
      
      for (Group g : destinations) {
         hash += (long) Math.pow(2, g.getId());
      }
      return hash;
   }
   
   boolean checkMessageAndEnqueue(byte[] msg, URPDeliveryMetadata deliveryMetadata, long t_batch_ready, long batch_serial_start,
         long batch_serial_end, long t_learner_delivered) {
      
      boolean localNodeIsDestination = false;
      ByteBuffer mb = ByteBuffer.wrap(msg);
      int ndests = mb.getInt();
      for (int i = 0 ; i < ndests && mb.hasRemaining() ; i++) {
         int dest = mb.getInt();
         if (dest == this.localGroup.getId())
            localNodeIsDestination = true;
      }
      
      if (localNodeIsDestination) {
         byte[] strippedMsg = Arrays.copyOfRange(msg, 4 + ndests * 4, msg.length);
         if (deserializeToMessage) {
            Message deserializedMsg = Message.createFromBytes(strippedMsg); // cmdContainer
//            deserializedMsg.t_batch_ready = t_batch_ready;
//            deserializedMsg.piggyback_proposer_serialstart = batch_serial_start;
//            deserializedMsg.piggyback_proposer_serialend   = batch_serial_end;
            deserializedMsg.t_learner_delivered = t_learner_delivered;
//            deserializedMsg.t_learner_deserialized = System.currentTimeMillis();
            deserializedMsg.setAttachment(deliveryMetadata);
            messageDeliveryQueue.add(deserializedMsg);
         }
         else {
            byteArrayDeliveryQueue.add(strippedMsg);
         }
      }
      
      return localNodeIsDestination;
   }
   
   URPRingData retrieveMappedRing(List<Group> destinations) {
      long destsHash = hashDestinationSet(destinations);
      URPRingData mappedRing = mappingGroupsToRings.get(destsHash);
      return mappedRing;
   }
   
   URPRingData retrieveMappedRing(Group destination) {
      long destHash = (long) Math.pow(2, destination.getId()); // just hashing right here
      URPRingData mappedRing = mappingGroupsToRings.get(destHash);
      return mappedRing;
   }
   
   synchronized private void sendToRing(URPRingData ring, ByteBuffer message) {
      try {
         message.flip();
         while(message.hasRemaining())
            ring.coordinatorConnection.write(message);
      }
      catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   // ****************** MESSAGE FORMAT ******************
   // | MESSAGE LENGTH (not counting length header) | NUMBER n OF DEST GROUPS | GROUPS | PAYLOAD |
   // |                4 bytes                      |         4 bytes         |  4*n   |   rest  |

   
//   @Override
   public void multicast(Group singleDestination, byte [] message) {
      ArrayList<Group> dests = new ArrayList<Group>(1);
      dests.add(singleDestination);
      multicast(dests, message);
   }
   
//   @Override
   public void multicast(List<Group> destinations, byte [] message) {
      multicaster.enqueueForMulticasting(destinations, message);
   }
   
   public void actuallyMulticast(List<Group> destinations, byte [] message) {
      int messageLength = 4 + 4 + 4*destinations.size() + message.length;
      ByteBuffer extMsg = ByteBuffer.allocate(messageLength);
      extMsg.putInt(messageLength - 4); // length in first header doesn't include that header's length
      extMsg.putInt(destinations.size());
      for (Group g : destinations)
         extMsg.putInt(g.getId());
      extMsg.put(message);
      
      URPRingData destinationRing = retrieveMappedRing(destinations);
      sendToRing(destinationRing, extMsg);
   }

//   @Override
   public byte [] deliver() {
      if (deserializeToMessage) {
         log.error("!!! - tried to deliver byte[] when Multicast Agent is configured to deliver Message.");
         return null;
      }
         
      byte[] msg = null; 
      try {
         msg = byteArrayDeliveryQueue.take();
      }
      catch (InterruptedException e) {
         e.printStackTrace();
      }
      return msg;
   }
   
   @Override
   public Message deliverMessage() {
      if (!deserializeToMessage) {
         log.error("!!! - tried to deliver Message when Multicast Agent is configured to deliver byte[].");
         return null;
      }
      
      Message msg = null;      
      try {
         msg = messageDeliveryQueue.take();
      }
      catch (InterruptedException e) {
         e.printStackTrace();
      }      
      return msg;
   }
   
   public boolean isDeserializingToMessage() {
      return deserializeToMessage;
   }
   
   // to translate from Group (.id) to whatever this implementation uses to represent a group
   // void addMapping(Group g, whatever urp uses inside to represent a group)
   
   // set up whatever configuration this specific mcast agent needs
   // *** TODO: check how gaps in the groups and rings sequence affects correctness
   // *** TODO: has to make sure that the max group is known before creating URPRings
   @SuppressWarnings("unchecked")
   // TODO: Using legacy API in the following method (Iterator part)
   public void loadURPAgentConfig(String filename, boolean hasLocalGroup, int nodeId) {
      
      // create all groups
      // create all rings
      // map rings to groups
      // map groups to rings
      // if mcagent is for a server (i.e., there is a local group)
      //     create a learner in the rings associated with that group
      //     such learner will put its messages in the delivery queue of the mcagent
      //     (or the deliver() from the mcagent could pop from the learner and discard group info)
      //     (or not)
      // create a connection to every ring's proposer
      
      
      
      try {
         
         // =====================================
         // Common settings
                  
         // ===========================================
         // Creating Groups

         JSONParser parser = new JSONParser();
         
         Object     nodeObj = parser.parse(new FileReader(filename));
         JSONObject config  = (JSONObject) nodeObj;
         
         Group.changeGroupImplementationClass(URPGroup.class);
         
         @SuppressWarnings("unused")
         boolean fastRing = false;
         if (config.containsKey("fast_ring")) {
            fastRing = (Boolean) config.get("fast_ring");
         }
         
         if (config.containsKey("multi_ring_m")) {
            int M = ((Long) config.get("multi_ring_m")).intValue();
            setMergeBlockSize(M);
         }
         
         if (config.containsKey("deserialize_to_Message")) {
            Boolean deserializeToMessageField = (Boolean) config.get("deserialize_to_Message");
            deserializeToMessage = deserializeToMessageField;
         }
         
         JSONArray groupsArray = (JSONArray) config.get("groups");         
         Iterator<Object> it_group = groupsArray.iterator();

         while (it_group.hasNext()) {
            JSONObject jsgroup = (JSONObject) it_group.next();
            long group_id = (Long) jsgroup.get("group_id");
            
            URPGroup group = (URPGroup) Group.getOrCreateGroup((int) group_id);
            
            log.info("Done creating group " + group.getId());
         }
         
         
         
         // ----------------------------------------------
         // creating zoo_host string in the format ip:port
         
         String zoo_host;
         JSONObject zoo_data = (JSONObject) config.get("zookeeper");
         zoo_host  = (String) zoo_data.get("location");
         zoo_host += ":";
         zoo_host += ((Long) zoo_data.get("port")).toString();
         log.info("Setting zoo_host as " + zoo_host);

         
         
         // ===========================================
         // Creating Ring Info

         JSONArray ringsArray = (JSONArray) config.get("rings");         
         Iterator<Object> it_ring = ringsArray.iterator();

         while (it_ring.hasNext()) {
            JSONObject jsring  = (JSONObject) it_ring.next();
            int ring_id = Util.getJSInt(jsring, "ring_id");
            
            URPRingData ringData = new URPRingData(ring_id);
            URPRingWatcher ringWatcher = new URPRingWatcher(ring_id, zoo_host);
            ringData.setWatcher(ringWatcher);
            if (hasLocalGroup == false) ringWatcher.waitForInitialLearnersInfo();
            
            JSONArray destGroupsArray = (JSONArray) jsring.get("destination_groups");
            Iterator<Object> it_destGroup = destGroupsArray.iterator();
            
            while (it_destGroup.hasNext()) {
               long destGroupId = (Long) it_destGroup.next();
               URPGroup destGroup= (URPGroup) URPGroup.getGroup((int) destGroupId);
               ringData.addDestinationGroup(destGroup);
               destGroup.addAssociatedRing(ringData);
            }
                        
            log.info("Done creating ringdata for ring " + ringData.getId());
         }
         
         
         
         // ===========================================
         // Mapping destination sets do rings
                  
         mapGroupsToRings();
         
         
         
         // ===========================================
         // Checking ring nodes
         
         JSONArray nodesArray = (JSONArray) config.get("ring_nodes");
         Iterator<Object> it_node = nodesArray.iterator();

         while (it_node.hasNext()) {
            JSONObject jsnode    = (JSONObject) it_node.next();
            
//            long      nodeId       = (Long)      jsnode.get("node_id");
            String    nodeLocation = (String)    jsnode.get("node_location");
            
            JSONArray nodeRings    = (JSONArray) jsnode.get("node_rings");
            Iterator<Object> it_nodeRing = nodeRings.iterator();

            while (it_nodeRing.hasNext()) {
               JSONObject jsnodering = (JSONObject) it_nodeRing.next();

               long      ring_id   = (Long)      jsnodering.get("ring_id");
               JSONArray nodeRoles = (JSONArray) jsnodering.get("roles");

               Iterator<Object> it_nodeRole = nodeRoles.iterator();
               
               boolean isCoordinator = false;
               while (it_nodeRole.hasNext()) {
                  String roleString = (String) it_nodeRole.next();
                  if (roleString.equals("proposer"))
                     isCoordinator = true;
               }
               if (isCoordinator) {
                  long nodeProposerPort = (Long) jsnodering.get("proposer_port");
                  URPRingData nodeRing  = URPRingData.getById((int) ring_id);
                  nodeRing.setCoordinator(nodeLocation, (int) nodeProposerPort);
                  log.info("Set Ring " + nodeRing.getId() + " to proposer at "
                                     + nodeLocation + ":" + nodeProposerPort);
               }
               
            }
         }

         
         
         // ===========================================
         // Connect to all ring coordinators
         for (URPRingData ring : URPRingData.ringsList)
            ring.connectToCoordinator();
         
         
         
         // ==========================================
         // Creating the learner in all relevant rings
         
         // The "learners" object is used to specify server ports and the group of each learner
         // If the local node is a learner, this will set the variable group
         int localGroupId = -1;
         boolean hasLearners = config.containsKey("learners");
         if (hasLearners) {
            JSONArray learnersArray = (JSONArray) config.get("learners");
            Iterator<Object> it_learner = learnersArray.iterator();
            while (it_learner.hasNext()) {
               JSONObject jslearner = (JSONObject) it_learner.next();
               int learner_id = Util.getJSInt(jslearner, "learner_id");
               int group_id   = Util.getJSInt(jslearner, "group_id");
               if (learner_id == nodeId) localGroupId = group_id;
               String learner_location = (String) jslearner.get("learner_location");
               int learner_port = Util.getJSInt(jslearner, "learner_port");
               URPMcastServerInfo.addServerToMap(learner_id, group_id, learner_location, learner_port);
               URPGroup group = (URPGroup) Group.getGroup(group_id);
               group.addMember(learner_id);
            }
         }

         if (hasLocalGroup) {

            URPGroup localGroup = (URPGroup) Group.getGroup(localGroupId);
            setLocalGroup(localGroup);

            // ----------------------------------------------
            // creating list of ringdescriptors
            // (just for this learner; other ring nodes also have to parse their urp string)
            // *** ASSUME that the learner has the same id in ALL rings
            List<RingDescription> localURPaxosRings = new ArrayList<RingDescription>();
            for (URPRingData ringData : localGroup.associatedRings) {
               int ringId = ringData.getId();
               ArrayList<PaxosRole> pxRoleList = new ArrayList<PaxosRole>();
               pxRoleList.add(PaxosRole.Learner);
               localURPaxosRings.add(new RingDescription(ringId, nodeId, pxRoleList));
               log.info("Setting local node as learner in ring " + ringData.getId());
            }

            // ----------------------------------------------
            // Creating Paxos node from list of ring descriptors
//            URPaxosNode = fastRing ? new FastNode(zoo_host, localURPaxosRings) : new Node(zoo_host, localURPaxosRings);
            URPaxosNode = new Node(zoo_host, localURPaxosRings);
            URPaxosNode.start();
            Runtime.getRuntime().addShutdownHook(new Thread() {
               @Override
               public void run() {
                  try {
                     URPaxosNode.stop();
                  } catch (InterruptedException e) {
                  }
               }
            });
            
            // attach a learner thread to URPaxosNode to receive the messages from the rings
            urpAgentLearner = new URPAgentLearner(this, URPaxosNode, nodeId);
            
            // create mcastserver if that is the case
            if (hasLearners) {
               int port = URPMcastServerInfo.getServer(nodeId).getPort();
               urpMcastServer = new URPMulticastServer(this, urpAgentLearner, port);
            }
         }
         
         

      } catch (IOException | ParseException | InterruptedException | KeeperException e) {
         e.printStackTrace();
         System.exit(1);
      }      
      
   }
   
   public URPMulticastServer getMulticastServer() {
      return urpMcastServer;
   }

   @Override
   public Group getLocalGroup() {
      return this.localGroup;
   }
   
   public void setLocalGroup(URPGroup g) {
      this.localGroup = g;
//      ArrayList<URPRingData> correspondingRings = g.getCorrespondingRings();
      
   }

   @Override
   public void multicast(Group single_destination, Message message) {
      multicast(single_destination, message.getBytes());
   }

   @Override
   public void multicast(List<Group> destinations, Message message) {
      multicast(destinations, message.getBytes());
   }

   @Override
   public void notifyCheckpointMade(DeliveryMetadata deliveryToKeep) {
      URPDeliveryMetadata delivery = (URPDeliveryMetadata) deliveryToKeep;
      /*
       1 - get highest x multiple of M, such that x < delivery.instanceId (-1, because we're keeping instanceId)
       2 - set all rings subscribed by this learner with safe (x)
      */
      long previousInstance = delivery.instanceId - 1L;
      long safeInstance = previousInstance - (previousInstance % multiRingMergeBlock);
      Learner learner = URPaxosNode.getLearner();
      for (URPRingData urd : localGroup.associatedRings) {
         System.out.println(String.format(this.getClass().getSimpleName() + " :: setting instance %d or ring %d as safe", safeInstance, urd.ringId));
         learner.setSafeInstance(urd.ringId, safeInstance);
      }
   }

   @Override
   public boolean hasWholeDeliveryPreffix() {
      int firstRing = Integer.MAX_VALUE;
      for (URPRingData rd : localGroup.associatedRings)
         if (rd.ringId < firstRing) firstRing = rd.ringId;
      
      return firstDeliveryMetadata.ringId == firstRing && firstDeliveryMetadata.instanceId == 1;
   }

}
