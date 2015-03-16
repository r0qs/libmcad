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
            
         } catch (ClassNotFoundException | IOException e) {
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
