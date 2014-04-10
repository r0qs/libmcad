package ch.usi.dslab.bezerra.mcad.ridge;

import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ch.usi.dslab.bezerra.mcad.FastMulticastAgent;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.OptimisticMulticastAgent;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.Acceptor;
import ch.usi.dslab.bezerra.ridge.AcceptorSequence;
import ch.usi.dslab.bezerra.ridge.Client;
import ch.usi.dslab.bezerra.ridge.Coordinator;
import ch.usi.dslab.bezerra.ridge.Coordinator.Batcher;
import ch.usi.dslab.bezerra.ridge.DeliverInterface;
import ch.usi.dslab.bezerra.ridge.Ensemble;
import ch.usi.dslab.bezerra.ridge.Learner;
import ch.usi.dslab.bezerra.ridge.RidgeMessage;
import ch.usi.dslab.bezerra.ridge.RidgeMessage.MessageIdentifier;
import ch.usi.dslab.bezerra.ridge.RidgeMessage.Timestamp;

public class RidgeMulticastAgent implements MulticastAgent, OptimisticMulticastAgent, FastMulticastAgent {
   static Logger logger = LogManager.getLogger("RidgeMulticastAgent");
   
//   public static final Logger log = Logger.getLogger(RidgeMulticastAgent.class);
   RidgeGroup localGroup = null;
   static Map<Long, RidgeEnsembleData> mappingGroupsToEnsembles;
   BlockingQueue<byte[]>  byteArrayDeliveryQueue;
   BlockingQueue<Message> conservativeDeliveryQueue;
   BlockingQueue<Message> optimisticDeliveryQueue;
   BlockingQueue<Message> fastDeliveryQueue;
   // assume at most one group per process (although a group may be represented by multiple rings)
   static Map<Integer, RidgeGroup> processGroups = new ConcurrentHashMap<Integer, RidgeGroup>();

   // TODO: set those things
   ch.usi.dslab.bezerra.ridge.MulticastAgent ridgeMulticastAgent;
   ch.usi.dslab.bezerra.ridge.Process        localProcess;
   RidgeAgentLearner ridgeAgentLearner;
   int pid;

   public RidgeMulticastAgent(String configFile, int pid, boolean isInGroup) {
      byteArrayDeliveryQueue = new LinkedBlockingQueue<byte[]>();
      conservativeDeliveryQueue = new LinkedBlockingQueue<Message>();
      optimisticDeliveryQueue = new LinkedBlockingQueue<Message>();
      fastDeliveryQueue = new LinkedBlockingQueue<Message>();
      
      loadRidgeAgentConfig(configFile);
      
      // if this process is not in a group, it must be a client
      // (or some other multicaster from outside the multi-ensemble infrastructure)
      if (isInGroup) {
         RidgeGroup localGroup = getProcessGroup(pid);
         setLocalGroup(localGroup);
         
         Learner learner;
         localProcess = learner = (Learner) ch.usi.dslab.bezerra.ridge.Process.getProcess(pid);
         ridgeAgentLearner   = new RidgeAgentLearner(this, learner);
         ridgeMulticastAgent = new ch.usi.dslab.bezerra.ridge.MulticastAgent(learner);
      }
      // if this process is in a group, it must be a learner in at least one ensemble,
      // not needing to send any credentials
      else {
         localProcess = new Client(pid);
         ridgeMulticastAgent = ((Client) localProcess).getMulticastAgent();
      }
      localProcess.startRunning();
   }
   
   static RidgeGroup getProcessGroup(int pid) {
      return processGroups.get(pid);
   }

   static void mapGroupsToEnsembles() {
      mappingGroupsToEnsembles = new ConcurrentHashMap<Long, RidgeEnsembleData>();

      ArrayList<Group> groups = Group.getAllGroups();

      // sort the groups list by group_id
      Collections.sort(groups, new Comparator<Group>() {
         @Override
         public int compare(Group g1, Group g2) {
            return g1.getId() - g2.getId();
         }
      });
      // =================================

      logger.info("about to call recursive mapping with groups in " + groups);
      recursivelyMapAllGroupCombinations(groups, new ArrayList<Group>(), -1, false, 0);

   }

   static void recursivelyMapAllGroupCombinations(ArrayList<Group> all, ArrayList<Group> destsPrevious, int curId, boolean curGroupIsPresent, long hash) {
      if (all.isEmpty())
         return;

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
         if (destinations.isEmpty() || mappingGroupsToEnsembles.containsKey(hash))
            return;

         // iterate through all ensembles that deliver to all those groups, leaving
         // only those that work,
         // then
         // sort them by number of associated groups
         // return the first one (i.e., the ensemble with the least number of associated groups)
         ArrayList<RidgeEnsembleData> candidates = new ArrayList<RidgeEnsembleData>(RidgeEnsembleData.ensemblesList);

         // getting a valid list of candidate ensembles
         next_candidate: for (RidgeEnsembleData red : RidgeEnsembleData.ensemblesList) {
            for (Group g : destinations) {
               if (red.destinationGroups.contains(g) == false) {
                  candidates.remove(red);
                  continue next_candidate;
               }
            }
         }

         // sorting the candidate ensembles by number of associated groups
         Collections.sort(candidates, new Comparator<RidgeEnsembleData>() {
            @Override
            public int compare(RidgeEnsembleData e1, RidgeEnsembleData e2) {
               return e1.destinationGroups.size() - e2.destinationGroups.size();
            }
         });

         // getting the best candidate ensemble (i.e., that with the least number of
         // associated groups)
         // and indexing it for this set of destinations
         RidgeEnsembleData bestCandidateEnsemble = candidates.isEmpty() ? null : candidates.get(0);
         mappingGroupsToEnsembles.put(hash, bestCandidateEnsemble);

         logger.info("Added mapping of destination set " + destinations + " (with hash " + hash + ") to ensemble "
               + (bestCandidateEnsemble == null ? "null" : bestCandidateEnsemble.getId()));

      } else {
         // hash became : hash + 2 ^ curId
         recursivelyMapAllGroupCombinations(all, destinations, curId + 1, true, hash);
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

   RidgeEnsembleData retrieveMappedEnsemble(List<Group> destinations) {
      long destsHash = hashDestinationSet(destinations);
      RidgeEnsembleData mappedEnsemble = mappingGroupsToEnsembles.get(destsHash);
      return mappedEnsemble;
   }

   RidgeEnsembleData retrieveMappedEnsemble(Group destination) {
      long destHash = (long) Math.pow(2, destination.getId()); // just hashing
                                                               // right here
      RidgeEnsembleData mappedEnsemble = mappingGroupsToEnsembles.get(destHash);
      return mappedEnsemble;
   }

   synchronized private void sendToEnsemble(RidgeMessage m, RidgeEnsembleData ring) {
      ridgeMulticastAgent.multicast(m, ring.getEnsemble());
   }

   static int getJSInt(JSONObject jsobj, String fieldName) {
      return ((Long) jsobj.get(fieldName)).intValue();
   }
   
   static int getInt(Object obj) {
      return ((Long) obj).intValue();
   }
   
   // to translate from Group (.id) to whatever this implementation uses to
   // represent a group
   // void addMapping(Group g, whatever urp uses inside to represent a group)

   // set up whatever configuration this specific mcast agent needs
   // *** TODO: check how gaps in the groups and rings sequence affects
   // correctness
   // *** TODO: has to make sure that the max group is known before creating
   // URPRings
   @SuppressWarnings("unchecked")
   // TODO: Using legacy API in the following method (Iterator part)
   public static void loadRidgeAgentConfig(String filename) {

      // create all groups
      // create all ensembles
      // map ensembles to groups
      // map groups to ensembles
      // if mcagent is for a server (i.e., there is a local group)
      // create a learner in the rings associated with that group
      // such learner will put its messages in the delivery queue of the mcagent
      // (or the deliver() from the mcagent could pop from the learner and
      // discard group info)
      // (or not)
      // create a connection to every ring's proposer

      try {

         // =====================================
         // Common settings

         // ===========================================
         // Creating Groups

         JSONParser parser = new JSONParser();

         Object nodeObj = parser.parse(new FileReader(filename));
         JSONObject config = (JSONObject) nodeObj;

         Group.changeGroupImplementationClass(RidgeGroup.class);

         int batchSizeThreshold = getJSInt(config, "batch_size_threshold_bytes");
         Batcher.setMessageSizeThreshold(batchSizeThreshold);

         int batchTimeThreshold = getJSInt(config, "batch_time_threshold_ms");
         Batcher.setMessageSizeThreshold(batchTimeThreshold);
         
         JSONArray groupsArray = (JSONArray) config.get("groups");
         Iterator<Object> it_group = groupsArray.iterator();

         while (it_group.hasNext()) {
            JSONObject jsgroup = (JSONObject) it_group.next();
            long group_id = (Long) jsgroup.get("group_id");

            RidgeGroup group = (RidgeGroup) Group.getOrCreateGroup((int) group_id);

            logger.info("Done creating group {}", group.getId());
         }

         // ===========================================
         // Creating Ensemble Info

         JSONArray ensemblesArray = (JSONArray) config.get("ensembles");
         Iterator<Object> it_ensemble = ensemblesArray.iterator();

         while (it_ensemble.hasNext()) {
            JSONObject jsensemble = (JSONObject) it_ensemble.next();
            int ensemble_id = getJSInt(jsensemble, "ensemble_id");
            
            Ensemble ensemble = Ensemble.getOrCreateEnsemble(ensemble_id);

            RidgeEnsembleData ensembleData = new RidgeEnsembleData(ensemble_id, ensemble);

            String learnerBroadcastMode = (String) jsensemble.get("learner_broadcast_mode");
            if (learnerBroadcastMode.equals("DYNAMIC"))
               ensemble.setConfiguration(Ensemble.DYNAMIC);
            else if (learnerBroadcastMode.equals("RING"))
               ensemble.setConfiguration(Ensemble.RING);
            else
               ensemble.setConfiguration(Ensemble.RING);
            
            JSONArray destGroupsArray = (JSONArray) jsensemble.get("destination_groups");
            Iterator<Object> it_destGroup = destGroupsArray.iterator();

            while (it_destGroup.hasNext()) {
               int destGroupId = getInt(it_destGroup.next());
               RidgeGroup destGroup = (RidgeGroup) RidgeGroup.getGroup(destGroupId);
               ensembleData.addDestinationGroup(destGroup);
               destGroup.addAssociatedEnsemble(ensembleData);
            }

            logger.info("Done creating ensemble data for ensemble {}", ensembleData.getId());
         }

         // ===========================================
         // Mapping destination sets to ensembles

         mapGroupsToEnsembles();

         // ===========================================
         // Checking ensemble processes

         JSONArray processesArray = (JSONArray) config.get("ensemble_processes");
         Iterator<Object> it_process = processesArray.iterator();

         while (it_process.hasNext()) {
            JSONObject jsnode = (JSONObject) it_process.next();

            String role       = (String)  jsnode.get("role"    );
            int    pid        = getJSInt (jsnode,    "pid"     );
            int    ensembleId = getJSInt (jsnode,    "ensemble");
            String host       = (String)  jsnode.get("host"    );
            int    port       = getJSInt (jsnode,    "port"    );
            
            Ensemble ensemble = Ensemble.getEnsemble(ensembleId);
            
            if (role.equals("coordinator")) {
               Coordinator coordinator = new Coordinator(pid, host, port);
               ensemble.setCoordinator(coordinator);
               coordinator.setEnsemble(ensemble);
            }
            
            if (role.equals("acceptor")) {
               Acceptor acc  = new Acceptor (pid, host, port);
            }
         }
         
         // ===========================================
         // Checking acceptor sequences

         JSONArray acceptorSequencesArray = (JSONArray) config.get("acceptor_sequences");
         Iterator<Object> it_accseq = acceptorSequencesArray.iterator();
         
         while (it_accseq.hasNext()) {
            JSONObject asnode = (JSONObject) it_accseq.next();
            
            int     id         = getJSInt(asnode, "id");
            int     ensembleId = getJSInt(asnode, "ensemble_id");
            boolean coordWrite = (Boolean) asnode.get("coordinator_writes");
            
            System.out.println(id + " " + ensembleId + " " + coordWrite);
            
            AcceptorSequence acceptorSequence = new AcceptorSequence(id, ensembleId, coordWrite);
            
            System.out.println("Creating acceptor sequence " + id);
            JSONArray acceptors = (JSONArray) asnode.get("acceptors");
            Iterator<Object> it_acceptor = acceptors.iterator();
            while(it_acceptor.hasNext()) {
               int apid = getInt(it_acceptor.next());
               System.out.println("Acceptor in sequence " + id + " : " + apid);
               Acceptor acceptor = (Acceptor) Acceptor.getProcess(apid);
               acceptorSequence.addAcceptor(acceptor);
            }
            
         }
         
         // ===========================================
         // Checking groupmembers
         
         JSONArray groupMembersArray = (JSONArray) config.get("group_members");
         Iterator<Object> it_groupMember = groupMembersArray.iterator();
         
         while (it_groupMember.hasNext()) {
            JSONObject gmnode = (JSONObject) it_groupMember.next();
            
            int    pid  = getJSInt(gmnode,    "pid"  );
            int    gid  = getJSInt(gmnode,    "group");
            String host = (String) gmnode.get("host" );
            int    port = getJSInt(gmnode,    "port" );

            RidgeGroup rgroup = (RidgeGroup) RidgeGroup.getGroup(gid);

            processGroups.put(pid, rgroup);
            
            List<RidgeEnsembleData> groupEnsemblesData = rgroup.getCorrespondingRings();
            Learner learner = new Learner(pid, host, port);
            for (RidgeEnsembleData red : groupEnsemblesData){
               learner.subscribeToEnsemble(red.ensemble);               
            }
         }
         

      } catch (IOException | ParseException e) {
         e.printStackTrace();
         System.exit(1);
      }

   }

   @Override
   public Group getLocalGroup() {
      return this.localGroup;
   }

   public void setLocalGroup(RidgeGroup g) {
      this.localGroup = g;
      ArrayList<RidgeEnsembleData> correspondingEnsembles = g.getCorrespondingRings();
   }

   @Override
   public void multicast(Group single_destination, Message message) {
      List<Group> dests = new ArrayList<Group>(1);
      dests.add(single_destination);
      multicast(dests, message);
   }

   @Override
   public void multicast(List<Group> destinations, Message message) {
      RidgeEnsembleData red = retrieveMappedEnsemble(destinations);
      List<Integer> groupIds = new ArrayList<Integer>(destinations.size());
      for (Group g : destinations)
         groupIds.add(g.getId());
      RidgeMessage wrapperMessage = new RidgeMessage(
            MessageIdentifier.getNextMessageId(this.pid),
            RidgeMessage.MESSAGE_MULTICAST,
            red.ensembleId,
            -1,
            new Timestamp(System.currentTimeMillis(), this.pid),
            groupIds,
            message);
      ridgeMulticastAgent.multicast(wrapperMessage, red.ensemble);
   }
   
   @Override
   public Message deliverMessage() {
      Message msg = null;
      try {
         msg = conservativeDeliveryQueue.take();
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      return msg;
   }

   @Override
   public Message deliverMessageOptimistically() {
      Message msg = null;
      try {
         msg = optimisticDeliveryQueue.take();
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      return msg;
   }

   @Override
   public Message deliverMessageFast() {
      Message msg = null;
      try {
         msg = fastDeliveryQueue.take();
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      return msg;
   }

}
