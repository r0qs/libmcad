package ch.usi.dslab.bezerra.mcad.minimal;

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
   static HashMap<Integer, MMANode> nodeMap;
   MinimalMcastAgent mcagent;
   boolean running = true;
   
   // network & stream data structures for outgoing data
   Socket socketToNode = null;
   OutputStream streamToNode = null;   
   ObjectOutputStream outToNode = null;
   
   // network & stream data structures for incoming data
   Socket socketFromNode = null;
   InputStream streamFromNode = null;
   ObjectInputStream inFromNode = null;
   
   Thread receiverThread = null;
   String address;
   int port;
   
   public MMANode(MinimalMcastAgent agent) {
      mcagent = agent;
      receiverThread = new Thread(this);
   }
   
   public static MMANode getNode(int id) {
      return nodeMap.get(id);
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
         
         outToNode.write(localnodeid_rawbytes);
         
      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

   }
   
   @Override
   public void run() {
      while (running) {
         try {
            byte[] newMessage = (byte[])inFromNode.readObject();
            mcagent.deliveredMessages.add(newMessage);
         } catch (ClassNotFoundException e) {
            e.printStackTrace();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }
   
   
}
