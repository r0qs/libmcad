package ch.usi.dslab.bezerra.mcad.minimal;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.Message;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;

public class MinimalMcastAgent implements MulticastAgent {
   int localNodeId;
   boolean running = true;
   
   ServerSocket nodeServerSocket = null;
   Thread connectionAcceptorThread = null; // to accept connections from other nodes
   BlockingQueue<byte[]> deliveredMessages; // the node threads receive and put it here
   
   public MinimalMcastAgent (String configFile) {
      deliveredMessages = new LinkedBlockingQueue<byte[]>();
      loadMinimalAgentConfig(configFile);
   }
   
   public void send(MMANode node, byte[] message) {
      try {
         node.checkConnection();      
         node.outToNode.writeObject(message);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void multicast(Group single_destination, byte[] message) {
      MMAGroup g = (MMAGroup) single_destination;
      for (MMANode node : g.nodeList)
         send(node, message);      
   }

   @Override
   public void multicast(List<Group> destinations, byte[] message) {
      for (Group g : destinations)
         multicast(g, message);
   }

   @Override
   public byte[] deliver() {
      try {
         return deliveredMessages.take();
      } catch (InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      return null;
   }
   
   @Override
   public Message deliverMessage() {
      return null;
   }
   
   @Override
   public boolean isDeserializingToMessage() {
      return false;
   }
   
   // to translate from Group (.id) to whatever this implementation uses to represent a group
   // void addMapping(Group g, whatever urp uses inside to represent a group)
   
   // set up whatever configuration this specific mcast agent needs:
   
  /*
    
   {
     "agent_class" : "MinimalMcastAgent" ,
     
     "nodes" : 
     [
       { "node_id" : 1 ,
         "address" : "localhost" ,
         "port"    : 50011
       } ,
       { "node_id" : 2 ,
         "address" : "localhost" ,
         "port"    : 50002
       }
     ] ,
     
     "groups" :
     [
       { 
         "group_id"    : 1 ,
         "group_nodes" : [1]
       } ,
       {
         "group_id"    : 2 ,
         "group_nodes" : [2]
       }
     ] ,
     
     "local_node_id" : 1
   }
    
   */
   public void loadMinimalAgentConfig(String filename) {
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
            
            MMAGroup group = (MMAGroup) Group.getOrCreateGroup((int) group_id);
            
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
      for (MMAGroup g : MMAGroup.groupList) {
         for (MMANode n : g.nodeList) {
            if (n.id == this.localNodeId)
               return g;
         }
      }
      return null;
   }

}
