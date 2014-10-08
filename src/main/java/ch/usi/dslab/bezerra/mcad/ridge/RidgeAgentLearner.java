package ch.usi.dslab.bezerra.mcad.ridge;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.DeliverInterface;
import ch.usi.dslab.bezerra.ridge.Learner;
import ch.usi.dslab.bezerra.ridge.RidgeMessage;

public class RidgeAgentLearner implements DeliverInterface {
   Logger logger = LogManager.getLogger("RidgeAgentLearner");
   
   RidgeMulticastAgent rmcAgent;
   Learner             learner;
   
   public RidgeAgentLearner(RidgeMulticastAgent rmcAgent, Learner learner) {
      this.rmcAgent = rmcAgent;
      this.learner  = learner;
      learner.setDeliverInterface(this);
   }
   
   @SuppressWarnings("unchecked")
   boolean checkIfLocalMessage(RidgeMessage message) {
      final int localGroupId = rmcAgent.getLocalGroup().getId();
      List<Integer> destinationGroupIds = (List<Integer>) message.getItem(0);
      if (destinationGroupIds.contains(localGroupId))
         return true;
      else
         return false;
   }
   
   Message getApplicationMessage(RidgeMessage wrapperMessage) {
      Message appMessage = (Message) wrapperMessage.getItem(1);
      appMessage.copyTimelineStamps(wrapperMessage);
      return appMessage;
   }

   @Override
   public void deliverConservatively(RidgeMessage wrappedMessage) {
      logger.info("/___ Learner delivered message {} conservatively", wrappedMessage.getId());
//      System.out.println("Learner delivered message " + wrappedMessage + " conservatively.");
      if (checkIfLocalMessage(wrappedMessage) == false) return;
      try {
         rmcAgent.conservativeDeliveryQueue.put(getApplicationMessage(wrappedMessage));
      } catch (InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

   @Override
   public void deliverOptimistically(RidgeMessage wrappedMessage) {
      logger.info("/___ Learner delivered message {} optimistically", wrappedMessage.getId());
//      System.out.println("Learner delivered message " + wrappedMessage + " optimistically.");
      if (checkIfLocalMessage(wrappedMessage) == false) return;
      try {
         rmcAgent.optimisticDeliveryQueue.put(getApplicationMessage(wrappedMessage));
      } catch (InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

   @Override
   public void deliverFast(RidgeMessage wrappedMessage) {
      logger.info("/___ Learner delivered message {} fast", wrappedMessage.getId());
//      System.out.println("Learner delivered message " + wrappedMessage + " fast.");
      if (checkIfLocalMessage(wrappedMessage) == false) return;
      try {
         rmcAgent.fastDeliveryQueue.put(getApplicationMessage(wrappedMessage));
      } catch (InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

}
