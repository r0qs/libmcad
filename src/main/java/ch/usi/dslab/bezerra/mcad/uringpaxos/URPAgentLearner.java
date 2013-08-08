package ch.usi.dslab.bezerra.mcad.uringpaxos;

import org.apache.log4j.Logger;

import ch.usi.da.paxos.api.PaxosNode;
import ch.usi.da.paxos.message.Value;
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
//            Decision d = paxos.getLearner().getDecisions().take();
            Value v = paxos.getLearner().getDecisions().take().getValue();
            if (!v.isSkip()) {
               byte[] msg = v.getValue();
               if (mcAgent.checkMessageDestinations(msg))
                  mcAgent.deliveryQueue.add(msg);
            }            
         } catch (InterruptedException e) {
            logger.error(e);
            System.exit(0);
         }
      }
   }

}
