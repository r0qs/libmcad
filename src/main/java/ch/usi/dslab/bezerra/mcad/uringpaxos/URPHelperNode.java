package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;

import ch.usi.da.paxos.api.PaxosNode;
import ch.usi.da.paxos.examples.Util;
import ch.usi.da.paxos.ring.Node;
import ch.usi.da.paxos.ring.RingDescription;

public class URPHelperNode {
   
   static Logger logger = Logger.getLogger(URPHelperNode.class);

   private static class HelperProposer implements Runnable {
      boolean running = true;
      PaxosNode paxos;
      BlockingQueue<byte[]> pendingMessages;      
      SelectorListener selectorListener;
      Thread helperProposerThread = null;

      public HelperProposer(PaxosNode paxos, int port) {
         this.paxos = paxos;
         pendingMessages = new LinkedBlockingQueue<byte[]>();
         selectorListener = new SelectorListener(this, port);
         
         helperProposerThread = new Thread(this);
         helperProposerThread.start();
      }
      
      public void stop() {
         running = false;
      }

      @Override
      public void run() {
         try {
            while (running) {
               byte[] proposal = pendingMessages.take();
               
               System.out.println("URPHelperProposer: proposing msg (+ destlist) length: " + proposal.length);
               
               String msg = new String(proposal, 8, proposal.length - 8);
               System.out.println("  |---> proposed message: " + msg);
               
               // The following 3 lines propose the _proposal_ in all rings this
               // node is a proposer in. However, the urpmcadaptor has a single,
               // different HelperProposer (coordinator) for each ring.
               for (RingDescription ring : paxos.getRings()) {                    
                  paxos.getProposer(ring.getRingID()).propose(proposal);
                  System.out.println("Proposed \"" + msg + "\" on ring " + ring.getRingID());
               }
               
            }
            selectorListener.running = false;
         } catch (InterruptedException e) {
            logger.error(e);
         }
      }
   }
   
   private static class SelectorListener implements Runnable {
      boolean running = true;
      Thread selectorListenerThread = null;
      
      HelperProposer helperProposer;
      
      ServerSocketChannel listener;
      Selector selector;
      HashMap<SocketChannel, ByteBuffer> bufferMap;
      
      public SelectorListener(HelperProposer hp, int port) {
         try {
            this.helperProposer = hp;
            
            this.selector = Selector.open();
            
            this.listener = ServerSocketChannel.open();
            listener.socket().bind(new InetSocketAddress(port));
            listener.configureBlocking(false);
            listener.register(selector, SelectionKey.OP_ACCEPT);
            
            bufferMap = new HashMap<SocketChannel, ByteBuffer>();
            
            selectorListenerThread = new Thread(this);
            selectorListenerThread.start();
         }
         catch (IOException e) {
            e.printStackTrace();
         }
      }

      @Override
      public void run() {
         try {
         while (running) {
            selector.select();
            Iterator<SelectionKey> i = selector.selectedKeys().iterator();

            while (i.hasNext()) {
               SelectionKey key = i.next();
               i.remove();
               
               // handle new connection
               if (key.isAcceptable()) {
               
                  SocketChannel mcaster_node = listener.accept();
                  mcaster_node.configureBlocking(false);
                  mcaster_node.socket().setTcpNoDelay(true);
                  mcaster_node.register(selector, SelectionKey.OP_READ);
                  ByteBuffer nodeBuffer = ByteBuffer.allocate(1048576);
                  bufferMap.put(mcaster_node, nodeBuffer);
                  System.out.println("new mcaster " + mcaster_node);
               }

               // handle new message
               if (key.isReadable()) {
                  processMessage(key);
               }
            }

         }
         } catch (IOException e) {
            e.printStackTrace();            
         }
      }
      
      void processMessage(SelectionKey key) {
         try {
            SocketChannel ch = (SocketChannel) key.channel();
            ByteBuffer buf = bufferMap.get(ch);
            ch.read(buf);
            buf.flip();
            while (hasCompleteMessage(buf)) {
               int length = buf.getInt();
               byte[] rawMessage = new byte[length];
               buf.get(rawMessage);
               helperProposer.pendingMessages.add(rawMessage);
            }
            buf.compact();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      
      boolean hasCompleteMessage(ByteBuffer buf) {
         int bytes = buf.limit() - buf.position();

         if (bytes < 4)
            return false;

         int length = buf.getInt();
         buf.position(buf.position() - 4);

         if (bytes < 4 + length)
            return false;

         return true;
      }
      
      static Object deserialize(byte[] buf) {
         ByteArrayInputStream bis = new ByteArrayInputStream(buf);
         ObjectInput in = null;
         Object obj = null;
         try {
            in = new ObjectInputStream(bis);
            obj = in.readObject();
         }
         catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
         finally {
            try {
               bis.close();
               in.close();
            } catch (IOException e) { e.printStackTrace(); }
         }
         return obj;
      }
   }
   
   
   
   private static class HookedObjectsContainer {
      Node           node;
      HelperProposer helperProposer;
      
      void stopAllHanged() {
         try {
            if (helperProposer != null) {
               System.out.println("Stopping the HelperProposer");
               helperProposer.stop();
            }
            if (node != null) {
               System.out.println("Stopping the Node");
               node.stop();
            }
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
   }
   
   
   

   public static void main (String args[]) {
      String zoo_host = args[0];
      String urpx_str = args[1];
      boolean isProposer = urpx_str.contains("P");
      
      List<RingDescription> ringdesc = Util.parseRingsArgument(urpx_str);
      
      final Node ringNode = new Node(zoo_host, ringdesc);
      HelperProposer hp = null;
      try {
         ringNode.start();
         
         Util.printRings(ringdesc);
         
         if (isProposer) {
            System.out.println("arguments: " + args[0] + " " + args[1] + " " + args[2]);
            int proposer_port = Integer.parseInt(args[2]);
            hp = new HelperProposer(ringNode, proposer_port);
         }
         
         final HookedObjectsContainer hanger = new HookedObjectsContainer();
         hanger.node = ringNode;
         hanger.helperProposer = hp;
         
         Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
               hanger.stopAllHanged();
               try {
                  ringNode.stop();
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
         });
      } catch (IOException | KeeperException | InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
      
      
      
   }
}
