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
   public void loadURPAgentConfig(String filename) {
      
      
      try {
         
         // =====================================
         // JSON Parser object
         
         JSONParser parser = new JSONParser();
         
         
         
         // =====================================
         // Node-specific settings
         
         Object nodeObj = parser.parse(new FileReader(filename));
         JSONObject nodeConfig = (JSONObject) nodeObj;         
         long localId = (Long) nodeConfig.get("local_node_id");
         String commonConfigFileName = (String) nodeConfig.get("common_config_file");
         
         
         
         // =====================================
         // Common settings
         
         Object obj = parser.parse(new FileReader(commonConfigFileName));
         JSONObject config = (JSONObject) obj;

         
         
         // ===========================================
         // Creating Nodes
         
         JSONArray nodesArray = (JSONArray) config.get("nodes");

         @SuppressWarnings("unchecked")
         // Using legacy API in the next line of code
         Iterator<Object> it_node = nodesArray.iterator();

         while (it_node.hasNext()) {
            JSONObject node = (JSONObject) it_node.next();
            long node_id = (Long) node.get("node_id");
            String address = (String) node.get("address");
            long port = (Long) node.get("port");
            MMANode newnode = new MMANode(this, (int) node_id);
            newnode.setAddress(address);
            newnode.setPort((int) port);
            
            System.out.println("created a node: id(" + newnode.id 
                               + "), address(" + newnode.address 
                               + "), port(" + newnode.port + ")");
         }
         
         
         
         // ===========================================
         // Creating Groups
         
         Group.changeGroupImplementationClass(MMAGroup.class);
         
         JSONArray groupsArray = (JSONArray) config.get("groups");
         
         @SuppressWarnings("unchecked")
         // Using legacy API in the next line of code
         Iterator<Object> it_group = groupsArray.iterator();

         while (it_group.hasNext()) {
            JSONObject jsgroup = (JSONObject) it_group.next();
            long group_id = (Long) jsgroup.get("group_id");
            JSONArray groupNodesArray = (JSONArray) jsgroup.get("group_nodes");
            
            MMAGroup group = (MMAGroup) Group.getGroup((int) group_id);
            
            @SuppressWarnings("unchecked")
            // Using legacy API in the next line of code
            Iterator<Object> it_groupNode = groupNodesArray.iterator();
    
            while (it_groupNode.hasNext()) {
               long groupNodeId = (Long) it_groupNode.next();
               MMANode node = MMANode.getNode((int) groupNodeId);
               group.addNode(node);
               System.out.println("Added node " + node.id + " to group " + group.getId());
            }
            
            System.out.println("Done creating group " + group.getId() + " with nodes " + group.nodeList);
         }
         
         
         
         // ===========================================
         // Creating LocalNode
        
         this.localNodeId = (int) localId;
         MMANode thisNode = MMANode.getNode(this.localNodeId);
                
         

         // ===========================================
         // Setting up the connection-listener thread     

         System.out.println("Setting up the local multicast" 
               + " agent to listen on port "
               + thisNode.getPort());
         
         nodeServerSocket = new ServerSocket(thisNode.getPort());
         connectionAcceptorThread = new MMAConnectionAcceptorThread(this);
         connectionAcceptorThread.start();
         
         System.out.println("Done setting up the local multicast"
               + " agent with a listening socket on port "
               + thisNode.getPort());

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

}
