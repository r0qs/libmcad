package ch.usi.dslab.bezerra.mcad.uringpaxos;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ch.usi.da.paxos.api.PaxosNode;
import ch.usi.da.paxos.message.Value;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class URPAgentLearner implements Runnable {
   public static final Logger log = Logger.getLogger(URPAgentLearner.class);
   PaxosNode paxos;
   URPMcastAgent mcAgent;
   Thread urpAgentLearnerThread;
   private final static Logger logger;
//   private final static Logger valuelogger;
   
   static {
      logger      = Logger.getLogger(URPMcastAgent.class);
//      valuelogger = Logger.getLogger(Value.class);
      log.setLevel(Level.OFF);
   }

   public URPAgentLearner(URPMcastAgent mcAgent, PaxosNode paxos) {
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
//                  mcAgent.checkMessageAndEnqueue(msg, batch.t_batch_ready,
//                        batch.piggyback_proposer_serialstart, batch.piggyback_proposer_serialend,
//                        t_learner_delivered);
                  mcAgent.checkMessageAndEnqueue(msg, 0, 0, 0, t_learner_delivered);
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
