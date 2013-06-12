package ch.usi.dslab.bezerra.mcad.minimal;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;

public class MinimalMcastAgent implements MulticastAgent {
   int localNodeId;
   
   ServerSocket nodeServerSocket = null;
   Thread connectionAcceptorThread = null; // to accept connections from other nodes
   BlockingQueue<byte[]> deliveredMessages; // the node threads receive and put it here
   
   public MinimalMcastAgent (String configFile) {
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
   public void multicast(ArrayList<Group> destinations, byte[] message) {
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
   
   // to translate from Group (.id) to whatever this implementation uses to represent a group
   // void addMapping(Group g, whatever urp uses inside to represent a group)
   
   // set up whatever configuration this specific mcast agent needs
   public void loadMinimalAgentConfig(String filename) {
      try {
         int port = 0; // get this port number from the config file
         nodeServerSocket = new ServerSocket(port);
         connectionAcceptorThread = new Thread(new Runnable() {
            
            @Override
            public void run() {
               try {
                  Socket connectionSocket = nodeServerSocket.accept();
                  
                  InputStream is = connectionSocket.getInputStream();  
                  ObjectInputStream ois = new ObjectInputStream(is);  
                  byte[] to = (byte[])ois.readObject();
                  ByteBuffer wrapped = ByteBuffer.wrap(to);
                  int remoteNodeId = wrapped.getInt();
                  
                  if (to!=null) {
                     System.out.println("Successfully got the byte array... or did it?");
                     System.out.println("node_id = " + remoteNodeId + "\n");
                  }
                  
                  MMANode node = MMANode.getNode(remoteNodeId);
                  node.socketFromNode = connectionSocket;
                  node.streamFromNode = is;
                  node.inFromNode = ois;
                  node.receiverThread.start();
                                    
                  /*
                  BufferedReader inFromUnknownNode =
                        new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                  String remoteNodeIdString = inFromUnknownNode.readLine();
                  int remoteNodeId = Integer.parseInt(remoteNodeIdString);
                  MMANode node = MMANode.getNode(remoteNodeId);
                  node.inFromNode = inFromUnknownNode;
                  node.receiverThread.start();
                  */
                  
                  
               } catch (IOException e) {
                  e.printStackTrace();
               } catch (ClassNotFoundException e) {
                  e.printStackTrace();
               }
               
            }
            
         });
         
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }      
   }

}
