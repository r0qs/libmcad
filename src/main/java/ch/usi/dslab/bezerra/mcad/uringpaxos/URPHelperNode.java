package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import ch.usi.da.paxos.api.PaxosNode;
import ch.usi.da.paxos.examples.TTYNode;
import ch.usi.da.paxos.ring.RingDescription;

public class URPHelperNode {

   static Logger logger = Logger.getLogger(TTYNode.class);

   private static class HelperProposer implements Runnable {
      PaxosNode paxos;
      BlockingQueue<byte[]> pendingMessages;
      ServerSocket listeningSocket;
      boolean running = true;
      ConnectionListener connectionListener;

      public HelperProposer(PaxosNode paxos, int port) {
         try {
            this.paxos = paxos;
            pendingMessages = new LinkedBlockingQueue<byte[]>();
            listeningSocket = new ServerSocket(port);
            connectionListener = new ConnectionListener(this);
            Thread t = new Thread(connectionListener);
            t.start();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      @Override
      public void run() {
         try {
            while (running) {
               byte[] proposal = pendingMessages.take();
               for (RingDescription ring : paxos.getRings()) {
                     paxos.getProposer(ring.getRingID()).propose(proposal);
               }
            }
            connectionListener.running = false;
         } catch (InterruptedException e) {
            logger.error(e);
         }
      }
   }
   
   private static class ConnectionListener implements Runnable {
      HelperProposer helperProposer;
      ArrayList<ProposerConnection> connectionList;
      boolean running = true;
      
      public ConnectionListener(HelperProposer hp) {
         this.helperProposer = hp;
         connectionList = new ArrayList<ProposerConnection>();
      }

      @Override
      public void run() {
         try {
            while (running) {
               Socket connectionSocket = helperProposer.listeningSocket.accept();
               ProposerConnection newconnection = new ProposerConnection(helperProposer, connectionSocket);
               connectionList.add(newconnection);
               Thread connthread = new Thread(newconnection);
               connthread.start();
            }
            for (ProposerConnection p : connectionList) {
               p.running = false;
            }
            
         } catch (IOException e) {
            e.printStackTrace();
         }
      }      
   }
   
   private static class ProposerConnection implements Runnable {
      HelperProposer helperProposer;
      Socket connectionSocket;
      boolean running = true;
      
      public ProposerConnection(HelperProposer hp, Socket connectionSocket) {
         this.helperProposer = hp;
         this.connectionSocket = connectionSocket;
      }
      
      @Override
      public void run() {
         InputStream is;
         ObjectInputStream ois;
         try {
            is = connectionSocket.getInputStream();
            ois = new ObjectInputStream(is);
            
            while (running) {
               byte[] new_message = (byte[]) ois.readObject();
               helperProposer.pendingMessages.add(new_message);
            }
            
         } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
         }
         
      }
      
   }

   public static void main (String args[]) {
      
   }
}
