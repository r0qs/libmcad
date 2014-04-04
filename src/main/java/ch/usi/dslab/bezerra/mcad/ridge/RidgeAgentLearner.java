package ch.usi.dslab.bezerra.mcad.ridge;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ch.usi.da.paxos.api.PaxosNode;
import ch.usi.da.paxos.message.Value;
import ch.usi.da.paxos.storage.Decision;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastAgentFactory;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.DeliverInterface;
import ch.usi.dslab.bezerra.ridge.Learner;
import ch.usi.dslab.bezerra.ridge.Process;
import ch.usi.dslab.bezerra.ridge.RidgeMessage;

public class RidgeAgentLearner implements DeliverInterface {
   public static final Logger log = Logger.getLogger(RidgeAgentLearner.class);

   Learner learner;
   
   RidgeMulticastAgent mcAgent;
   Thread ridgeAgentLearnerThread;
   private final static Logger logger;
   private final static Logger valuelogger;
   
   boolean running = true;
   
   static {
      logger      = Logger.getLogger(RidgeMulticastAgent.class);
      valuelogger = Logger.getLogger(Value.class);
      log.setLevel(Level.OFF);
   }

   public RidgeAgentLearner(RidgeMulticastAgent mcAgent, int pid) {
      this.mcAgent = mcAgent;
      this.learner = (Learner) Process.getProcess(pid);
      learner.setDeliverInterface(this);
   }

   @SuppressWarnings("unchecked")
   boolean checkIfLocalMessage(RidgeMessage message) {
      final int localGroupId = mcAgent.getLocalGroup().getId();
      List<Integer> destinationGroupIds = (List<Integer>) message.getItem(0);
      if (destinationGroupIds.contains(localGroupId))
         return true;
      else
         return false;
   }

   @Override
   public void deliverConservatively(RidgeMessage message) {
      if (checkIfLocalMessage(message) == false) return;
      try {
         mcAgent.conservativeDeliveryQueue.put(message);
      } catch (InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

   @Override
   public void deliverOptimistically(RidgeMessage message) {
      if (checkIfLocalMessage(message) == false) return;
      try {
         mcAgent.optimisticDeliveryQueue.put(message);
      } catch (InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

   @Override
   public void deliverFast(RidgeMessage message) {
      if (checkIfLocalMessage(message) == false) return;
      try {
         mcAgent.fastDeliveryQueue.put(message);
      } catch (InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

}
