package ch.usi.dslab.bezerra.mcad.ridge;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ch.usi.da.paxos.api.PaxosNode;
import ch.usi.da.paxos.message.Value;
import ch.usi.da.paxos.storage.Decision;
import ch.usi.dslab.bezerra.mcad.MulticastAgentFactory;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class RidgeAgentLearner implements Runnable {
   public static final Logger log = Logger.getLogger(RidgeAgentLearner.class);
   PaxosNode paxos;
   RidgeMulticastAgent mcAgent;
   Thread urpAgentLearnerThread;
   private final static Logger logger;
   private final static Logger valuelogger;
   
   static {
      logger      = Logger.getLogger(RidgeMulticastAgent.class);
      valuelogger = Logger.getLogger(Value.class);
      log.setLevel(Level.OFF);
   }

   public RidgeAgentLearner(RidgeMulticastAgent mcAgent, PaxosNode paxos) {
      this.mcAgent = mcAgent;
      this.paxos = paxos;
      urpAgentLearnerThread = new Thread(this);
      urpAgentLearnerThread.start();
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
               byte[] rawBatch = v.getValue();

               if (rawBatch.length == 0 ) {
                  // System.err.println("0 bytes decision!");
                  // System.exit(1);
                  continue;
               }

               long t_learner_delivered = System.currentTimeMillis();
               Message batch = Message.createFromBytes(rawBatch);
                              
               while (batch.hasNext()) {
                  byte[] msg = (byte []) batch.getNext(); // cmdContainer
                  mcAgent.checkMessageAndEnqueue(msg, batch.t_batch_ready,
                        batch.piggyback_proposer_serialstart, batch.piggyback_proposer_serialend,
                        t_learner_delivered);
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
