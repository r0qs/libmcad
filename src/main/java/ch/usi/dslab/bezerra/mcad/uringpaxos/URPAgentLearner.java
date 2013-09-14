package ch.usi.dslab.bezerra.mcad.uringpaxos;

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
      log.setLevel(Level.INFO);
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
               log.info("urpagentlearner: New multicast messge received");
               byte[] rawBatch = v.getValue();
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
