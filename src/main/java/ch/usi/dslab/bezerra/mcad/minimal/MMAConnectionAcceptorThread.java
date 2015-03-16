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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class MMAConnectionAcceptorThread extends Thread {   
   MinimalMcastAgent mcAgent = null;
   
   public MMAConnectionAcceptorThread (MinimalMcastAgent mmca) {
      this.mcAgent = mmca;
   }
   
   @Override
   public void run() {
      while (mcAgent.running) {
         try {
            Socket connectionSocket = mcAgent.nodeServerSocket.accept();

            System.out.println("Incoming connection; new socket: "
                  + connectionSocket);

            InputStream is = connectionSocket.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            byte[] to = (byte[]) ois.readObject();
            ByteBuffer wrapped = ByteBuffer.wrap(to);
            int remoteNodeId = wrapped.getInt();

            if (to != null) {
               System.out
                     .println("Successfully got the byte array... or did it?");
               System.out.println("node_id = " + remoteNodeId + "\n");
            }

            MMANode node = MMANode.getNode(remoteNodeId);
            if (node == null) {
               node = new MMANode(this.mcAgent, remoteNodeId);
               node.socketToNode = connectionSocket;
               node.streamToNode = node.socketToNode.getOutputStream();
               node.outToNode = new ObjectOutputStream(node.streamToNode);
            }
            node.socketFromNode = connectionSocket;
            node.streamFromNode = is;
            node.inFromNode = ois;
            node.receiverThread.start();

         } catch (IOException e) {
            e.printStackTrace();
         } catch (ClassNotFoundException e) {
            e.printStackTrace();
         }
      }
      
   }

}
