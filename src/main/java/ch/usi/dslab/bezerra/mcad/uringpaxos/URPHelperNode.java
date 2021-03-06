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
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;

import ch.usi.da.paxos.Util;
import ch.usi.da.paxos.api.PaxosNode;
import ch.usi.da.paxos.ring.Node;
import ch.usi.da.paxos.ring.RingDescription;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class URPHelperNode {
   
   public static final Logger log = Logger.getLogger(URPHelperNode.class);

   private static class HelperProposer implements Runnable {
      boolean running = true;
      PaxosNode paxos;
      BlockingQueue<byte[]> pendingMessages;      
      SelectorListener selectorListener;
      Thread helperProposerThread = null;
      boolean batchingEnabled;
      int batchSizeThreshold;
      int batchTimeThreshold;

      public HelperProposer(PaxosNode paxos, int port,
            boolean batchingEnabled, int batchSizeThreshold, int batchTimeThreshold) {

         log.setLevel(Level.OFF);
         
         this.paxos = paxos;
         pendingMessages = new LinkedBlockingQueue<byte[]>();
         
         this.batchingEnabled    = batchingEnabled;
         this.batchSizeThreshold = batchSizeThreshold;
         this.batchTimeThreshold = batchTimeThreshold;

         System.out.println(String.format("URPHelperProposer BATCHing parameters: batchEnabled = %b, batchSize = %d, batchTime = %d", this.batchingEnabled, this.batchSizeThreshold, this.batchTimeThreshold));

         selectorListener = new SelectorListener(this, port);
         helperProposerThread = new Thread(this);
         helperProposerThread.start();
      }
      
      public void stop() {
         running = false;
      }

      @Override
      public void run() {
/*
//         int sizeBatchThreshold = 16384; // 16k, to avoid "Buffer too small" of umrpaxos
         int sizeBatchThreshold = 30000; // 30k, discounting overheads (so it's not 32768)
//         int sizeBatchThreshold = 250000; // 250k, because thea actual buffer is 262144 bytes
         int timeBatchThreshold = 50;   // 50 milliseconds
//         int timeBatchThreshold = 5;      // 5 milliseconds
//         int timeBatchThreshold  = 0;   // disable batching
*/
         long lastBatchTime = System.currentTimeMillis();
         
         // ==============
         // TIMELINE STUFF
         long last_serialStart = 0;
//         long last_serialEnd = 0;
         // ==============         

         Message batch = new Message();
         try {
            while (running) {
               byte[] proposal = pendingMessages.poll(batchTimeThreshold, TimeUnit.MILLISECONDS);
               
               if (proposal != null)
                  batch.addItems(proposal);
               
               if (batch.count() == 0)
                  continue;
               
//               System.out.println("HelperProposer received a proposal of " + proposal.length + " bytes.");
               
               long now = System.currentTimeMillis();
               long elapsed = now - lastBatchTime;
               
               if (batchingEnabled == false
                     || elapsed > batchTimeThreshold
                     || batch.getByteArraysAggregatedLength() > batchSizeThreshold) {
//               if (elapsed > timeBatchThreshold || batch.getSerializedLength() > sizeBatchThreshold) {
//                  log.info("URPHelperProposer: proposing msg (+ destlist) length: " + batch.getSerializedLength());                  

                  // The following 3 lines propose the _proposal_ in all rings this
                  // node is a proposer in. However, the urpmcadaptor has a single,
                  // different HelperProposer (coordinator) for each ring.
//                  batch.t_batch_ready = now;
                  for (RingDescription ring : paxos.getRings()) {
                     if (last_serialStart != 0) {
//                        batch.piggyback_proposer_serialstart = last_serialStart;
//                        batch.piggyback_proposer_serialend   = last_serialEnd;
                     }
                     
                     last_serialStart = System.currentTimeMillis();
                     byte[] serialBatch = batch.getBytes();
//                     last_serialEnd   = System.currentTimeMillis();
                     
                     paxos.getProposer(ring.getRingID()).propose(serialBatch);
                  }
                  batch = new Message();
                  lastBatchTime = now;
               }

            }
            selectorListener.running = false;
         } catch (InterruptedException e) {
            log.error(e);
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
            int readyKeys = selector.select(250);
            if (readyKeys == 0) continue;

            Iterator<SelectionKey> i = selector.selectedKeys().iterator();

            while (i.hasNext()) {
               SelectionKey key = i.next();
               i.remove();
               if (!key.isValid())
                  continue;
               
               // handle new connection
               if (key.isAcceptable()) {               
                  SocketChannel mcaster_node = listener.accept();
                  mcaster_node.configureBlocking(false);
                  mcaster_node.socket().setTcpNoDelay(true);
                  mcaster_node.register(selector, SelectionKey.OP_READ);
                  ByteBuffer nodeBuffer = ByteBuffer.allocate(65536);
                  bufferMap.put(mcaster_node, nodeBuffer);
                  log.info("new mcaster " + mcaster_node);
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
            int readBytes = ch.read(buf);
       
            if (readBytes == -1) {
               bufferMap.remove(ch);
               ch.close();
               return;
            }
            
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
      
      @SuppressWarnings("unused")
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
               log.info("Stopping the HelperProposer");
               helperProposer.stop();
            }
            if (node != null) {
               log.info("Stopping the Node");
               node.stop();
            }
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
   }
   
   
   

   public static void main (String args[]) {
      log.info("URPHelperNodearguments: " + args);
      String zoo_host = args[0];
      String urpx_str = args[1];
      boolean isProposer = urpx_str.contains("P");
      @SuppressWarnings("unused")
      boolean fastRing = args[2].equalsIgnoreCase("fastring");
      
      List<RingDescription> ringdesc = Util.parseRingsArgument(urpx_str);
      
//      final Node ringNode = fastRing ? new FastNode(zoo_host, ringdesc) : new Node(zoo_host, ringdesc);
      final Node ringNode = new Node(zoo_host, ringdesc);
      HelperProposer hp = null;
      try {
         
         if (isProposer) {
            int proposer_port = Integer.parseInt(args[3]);
            boolean enable_batching = Boolean.parseBoolean(args[4]);
            int batch_size = Integer.parseInt(args[5]);
            int batch_time = Integer.parseInt(args[6]);
            hp = new HelperProposer(ringNode, proposer_port, enable_batching, batch_size, batch_time);
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
         
         ringNode.start();
         
      } catch (IOException | KeeperException | InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
      
   }
}
