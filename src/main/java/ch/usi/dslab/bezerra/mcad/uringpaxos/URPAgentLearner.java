package ch.usi.dslab.bezerra.mcad.uringpaxos;

import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ch.usi.da.paxos.api.PaxosNode;
import ch.usi.da.paxos.message.Value;
import ch.usi.da.paxos.storage.Decision;
import ch.usi.dslab.bezerra.mcad.Message;
import ch.usi.dslab.bezerra.mcad.MulticastAgentFactory;

public class URPAgentLearner implements Runnable {
   public static final Logger log = Logger.getLogger(URPAgentLearner.class);
   PaxosNode paxos;
   URPMcastAgent mcAgent;
   Thread urpAgentLearnerThread;
   private final static Logger logger;
   private final static Logger valuelogger;
   
   static {
      logger      = Logger.getLogger(URPMcastAgent.class);
      valuelogger = Logger.getLogger(Value.class);
      log.setLevel(Level.OFF);
   }

   public URPAgentLearner(URPMcastAgent mcAgent, PaxosNode paxos) {
      this.mcAgent = mcAgent;
      this.paxos = paxos;
      urpAgentLearnerThread = new Thread(this);
      urpAgentLearnerThread.start();
   }

   byte[] decompress(byte[] compressed) {
      LZ4Factory factory = LZ4Factory.fastestInstance();
      LZ4FastDecompressor decompressor = factory.fastDecompressor();
      int originalLength =  compressed[0] << 24
                             | (compressed[1] & 0xFF) << 16
                             | (compressed[2] & 0xFF) << 8
                             | (compressed[3] & 0xFF);
      byte[] original = new byte[originalLength];
      decompressor.decompress(compressed, 4, original, 0, originalLength);
      return original;
   }

   @Override
   public void run() {      
      if (paxos.getLearner() == null) {
         log.error("EEE === Not a learner");
         return; // not a learner
      }
      while (true) {
         try {
            Value v = paxos.getLearner().getDecisions().take().getValue();            
            if (!v.isSkip()) {
               byte[] compressedBatch = v.getValue();
               byte[] rawBatch        = decompress(compressedBatch);
               Message batch = Message.createFromBytes(rawBatch);
               
               while (batch.hasNext()) {
                  byte[] msg = (byte []) batch.getNext();
                  mcAgent.checkMessageAndEnqueue(msg);
               }               
            }            
         }
         catch (InterruptedException e) {
            logger.error(e);
            System.exit(0);
         }
      }
   }

}
