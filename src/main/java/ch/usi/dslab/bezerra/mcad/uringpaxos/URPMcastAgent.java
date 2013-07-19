package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.minimal.MMAConnectionAcceptorThread;
import ch.usi.dslab.bezerra.mcad.minimal.MMAGroup;
import ch.usi.dslab.bezerra.mcad.minimal.MMANode;

public class URPMcastAgent implements MulticastAgent {
   
   URPGroup localGroup = null;
   
   public URPMcastAgent (String configFile) {
      loadURPAgentConfig(configFile);
   }
   
   URPRingData mapRing(ArrayList<Group> destinations) {
      URPRingData currentRing = URPRingData.ringsList.get(0);
      URPRingData nextRing = null;
      
      while (true) {
         boolean checked_first = false;
         for (Group g : destinations) {
            if (checked_first == false) {
               nextRing = g.getId() < currentRing.pivot() ? currentRing.left() : currentRing.right();
               if (nextRing == null) return currentRing;
               checked_first = true;
            }
            else {
               URPRingData otherNextRing = g.getId() < currentRing.pivot() ? currentRing.left() : currentRing.right();
               if (otherNextRing != nextRing) return currentRing;
            }
         }
         currentRing = nextRing;
      }
   }

   @Override
   public void multicast(ArrayList<Group> destinations, byte [] message) {
      URPRingData destinationRing = mapRing(destinations);
   }
   
   @Override
   public void multicast(Group single_destinations, byte [] message) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public byte [] deliver() {
      // TODO Auto-generated method stub
      return null;
   }
   
   // to translate from Group (.id) to whatever this implementation uses to represent a group
   // void addMapping(Group g, whatever urp uses inside to represent a group)
   
   // set up whatever configuration this specific mcast agent needs
   // *** TODO: check how gaps in the groups and rings sequence affects correctness
   // *** TODO: has to make sure that the max group is known before creating URPRings
   @SuppressWarnings("unchecked")
   // TODO: Using legacy API in the following method (Iterator part)
   public void loadURPAgentConfig(String filename) {
      
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
         // JSON Parser object
         
         JSONParser parser = new JSONParser();
         
         
         
         // =====================================
         // This node's specific settings
         
         Object     nodeObj    = parser.parse(new FileReader(filename));
         JSONObject nodeConfig = (JSONObject) nodeObj;
                  
         String       nodeType = (String) nodeConfig.get("node_type");
         boolean      hasLocalGroup;
         long         serverId, localGroupId; 
         
         if (nodeType.equals("server")) {
            hasLocalGroup = true;
            serverId      = (Long) nodeConfig.get("server_id");
            localGroupId  = (Long) nodeConfig.get("server_group_id");
         }
         else if (nodeType.equals("client")) {
            hasLocalGroup = false;
         }
         
         String commonConfigFileName = (String) nodeConfig.get("common_config_file");
         
         
         
         // =====================================
         // Common settings
         
         Object obj = parser.parse(new FileReader(commonConfigFileName));
         JSONObject config = (JSONObject) obj;
         
         
         
         // ===========================================
         // Creating Groups
         
         Group.changeGroupImplementationClass(URPGroup.class);
         
         JSONArray groupsArray = (JSONArray) config.get("groups");         
         Iterator<Object> it_group = groupsArray.iterator();

         while (it_group.hasNext()) {
            JSONObject jsgroup = (JSONObject) it_group.next();
            long group_id = (Long) jsgroup.get("group_id");
            
            URPGroup group = (URPGroup) Group.getOrCreateGroup((int) group_id);
            
            System.out.println("Done creating group " + group.getId());
         }
         
         
         
         // ===========================================
         // Creating Ring Info

         JSONArray ringsArray = (JSONArray) config.get("rings");         
         Iterator<Object> it_ring = ringsArray.iterator();

         while (it_ring.hasNext()) {
            JSONObject jsring  = (JSONObject) it_ring.next();
            long       ring_id = (Long) jsring.get("ring_id");
            
            URPRingData ringData = new URPRingData((int) ring_id);
            
            JSONArray destGroupsArray = (JSONArray) jsring.get("destination_groups");
            Iterator<Object> it_destGroup = destGroupsArray.iterator();
            
            while (it_destGroup.hasNext()) {
               Long destGroupId = (Long) it_destGroup.next();
            }
            
            
            
            
            System.out.println("Done creating ringdata for ring " + ringData.getId());
         }
                  
         
         
         // ===========================================
         // Checking helper nodes
         
         JSONArray nodesArray = (JSONArray) config.get("helper_nodes");         
         Iterator<Object> it_node = nodesArray.iterator();

         while (it_node.hasNext()) {
            JSONObject jsnode    = (JSONObject) it_node.next();
            
            long      nodeId       = (Long)      jsnode.get("node_id");
            String    nodeLocation = (String)    jsnode.get("node_location");            
            
            JSONArray nodeRings    = (JSONArray) jsnode.get("node_rings");            
            Iterator<Object> it_nodeRing = nodeRings.iterator();
            
            while (it_nodeRing.hasNext()) {
               JSONObject jsnodering = (JSONObject) it_nodeRing.next();
               
               long      ring_id   = (Long)      jsnodering.get("ring_id");
               JSONArray nodeRoles = (JSONArray) jsnodering.get("roles");
               
               @SuppressWarnings("unchecked")
               // Using legacy API in the next line of code
               Iterator<Object> it_nodeRole = nodeRoles.iterator();
               
               boolean isProposer = false;
               while (it_nodeRole.hasNext()) {
                  String roleString = (String) it_nodeRole.next();
                  if (roleString.equals("proposer"))
                     isProposer = true;
               }
               if (isProposer) {
                  long nodeProposerPort = (Long) jsnodering.get("proposer_port");
                  URPRingData nodeRing  = URPRingData.getById((int) ring_id);
                  nodeRing.setProposerHelper(nodeLocation, (int) nodeProposerPort);
                  System.out.println("Set Ring " + nodeRing.getId() + " to proposer at "
                                     + nodeLocation + ":" + nodeProposerPort);
               }
               
            }
         }
         
//         // ===========================================
//         // Creating LocalNode
//        
//         this.localNodeId = (int) localId;
//         MMANode thisNode = MMANode.getNode(this.localNodeId);
                
         

//         // ===========================================
//         // Setting up the connection-listener thread     
//
//         System.out.println("Setting up the local multicast" 
//               + " agent to listen on port "
//               + thisNode.getPort());
//         
//         nodeServerSocket = new ServerSocket(thisNode.getPort());
//         connectionAcceptorThread = new MMAConnectionAcceptorThread(this);
//         connectionAcceptorThread.start();
//         
//         System.out.println("Done setting up the local multicast"
//               + " agent with a listening socket on port "
//               + thisNode.getPort());

      } catch (IOException e) {
         e.printStackTrace();
      } catch (ParseException e) {
         e.printStackTrace();         
      }
      
      
   }

   @Override
   public Group getLocalGroup() {
      // TODO Auto-generated method stub
      return null;
   }
   
   public void setLocalGroup(URPGroup g) {
      this.localGroup = g;
      ArrayList<URPRingData> correspondingRings = g.getCorrespondingRings();
      
   }

}
