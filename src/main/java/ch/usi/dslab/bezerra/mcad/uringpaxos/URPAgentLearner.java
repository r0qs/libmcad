package ch.usi.dslab.bezerra.mcad.uringpaxos;

import org.apache.log4j.Logger;

import ch.usi.da.paxos.api.PaxosNode;
import ch.usi.da.paxos.storage.Decision;

public class URPAgentLearner implements Runnable {
   PaxosNode paxos;
   URPMcastAgent mcAgent;
   Thread urpAgentLearnerThread;
   static Logger logger = Logger.getLogger(URPMcastAgent.class);

   public URPAgentLearner(URPMcastAgent mcAgent, PaxosNode paxos) {
      this.mcAgent = mcAgent;
      this.paxos = paxos;
      urpAgentLearnerThread = new Thread(this);
      urpAgentLearnerThread.start();
   }

   @Override
   public void run() {      
      if (paxos.getLearner() == null) {
         return; // not a learner
      }
      while (true) {
         try {
            Decision d = paxos.getLearner().getDecisions().take();
            if (!d.isSkip()) {
               d.getValue().getValue();
               // TODO: check if the localgroup of the mcagent is actually
               // a true destination for this message
            }            
         } catch (InterruptedException e) {
            logger.error(e);
            System.exit(0);
         }
      }
   }

}
