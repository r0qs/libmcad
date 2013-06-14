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
