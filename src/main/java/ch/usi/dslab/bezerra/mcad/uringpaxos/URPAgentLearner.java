package ch.usi.dslab.bezerra.mcad.uringpaxos;

import org.apache.log4j.Logger;

import ch.usi.da.paxos.api.PaxosNode;
import ch.usi.da.paxos.message.Value;
import ch.usi.da.paxos.storage.Decision;

public class URPAgentLearner implements Runnable {
   PaxosNode paxos;
   URPMcastAgent mcAgent;
   Thread urpAgentLearnerThread;
   private final static Logger logger;
   private final static Logger valuelogger;
   
   static {
      logger      = Logger.getLogger(URPMcastAgent.class);
      valuelogger = Logger.getLogger(Value.class);
      //logger.setLevel((Level) Level.OFF);
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
         System.out.println("EEE === Not a learner");
         return; // not a learner
      }
      while (true) {
         try {
            Value v = paxos.getLearner().getDecisions().take().getValue();            
            if (!v.isSkip()) {
               byte[] msg = v.getValue();
               mcAgent.checkMessageAndEnqueue(msg);
            }            
         }
         catch (InterruptedException e) {
            logger.error(e);
            System.exit(0);
         }
      }
   }

}
