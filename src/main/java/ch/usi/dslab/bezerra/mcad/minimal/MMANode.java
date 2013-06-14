package ch.usi.dslab.bezerra.mcad.minimal;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class MMANode implements Runnable {
   static MinimalMcastAgent mcagent;
   static HashMap<Integer, MMANode> nodeMap;
   
   static {
      nodeMap = new HashMap<Integer, MMANode>();
   }
   
   public static MMANode getNode(int id) {
      MMANode node = nodeMap.get(id);
      /*
      if (node == null) {
         node = new MMANode(null, id);
      }
      */
      return node;
   }
   
   
   
   String address;

   int id;
   ObjectInputStream inFromNode = null;
   ObjectOutputStream outToNode = null;

   int port;
   Thread receiverThread = null;
   boolean running = true;


   Socket socketFromNode = null;
   // network & stream data structures for outgoing data
   Socket socketToNode = null;

   InputStream streamFromNode = null;

   OutputStream streamToNode = null;

   public MMANode(MinimalMcastAgent agent, int id) {
      if (agent != null)
         mcagent = agent;
      this.id = id;
      this.receiverThread = new Thread(this);
      nodeMap.put(id, this);
   }

   void checkConnection() {
      if (outToNode != null)
         return;

      try {

         socketToNode = new Socket(this.address, this.port);
         streamToNode = socketToNode.getOutputStream();
         outToNode = new ObjectOutputStream(streamToNode);

         ByteBuffer localnodeid_wrapper = ByteBuffer.allocate(32);
         localnodeid_wrapper.putInt(mcagent.localNodeId);
         byte[] localnodeid_rawbytes = localnodeid_wrapper.array();

         outToNode.writeObject(localnodeid_rawbytes);

      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

   }

   public String getAddress() {
      return address;
   }

   public int getId() {
      return id;
   }

   public int getPort() {
      return port;
   }

   @Override
   public void run() {
      while (running) {
         try {
            byte[] newMessage = (byte[]) inFromNode.readObject();
            mcagent.deliveredMessages.add(newMessage);
         } catch (EOFException closedConnectionException) {
            // close this connection, and destroy this node (remove from the nodes map)
            try {
            this.socketFromNode.close();
            this.socketToNode.close();
            this.streamFromNode.close();
            this.socketToNode.close();
            this.inFromNode.close();
            this.outToNode.close();
            nodeMap.remove(this.id);
            this.running = false;
            System.out.println("Connection with node " + this.id + " closed by remote.");
            }
            catch (IOException e) {
               e.printStackTrace();
            }
            
         } catch (ClassNotFoundException e) {
            e.printStackTrace();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   public void setAddress(String address) {
      this.address = address;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setPort(int port) {
      this.port = port;
   }

}
