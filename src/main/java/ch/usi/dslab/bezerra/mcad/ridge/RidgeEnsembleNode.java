package ch.usi.dslab.bezerra.mcad.ridge;

import com.sun.media.jfxmedia.logging.Logger;

import ch.usi.dslab.bezerra.ridge.Coordinator;
import ch.usi.dslab.bezerra.ridge.DeliverInterface;
import ch.usi.dslab.bezerra.ridge.Learner;
import ch.usi.dslab.bezerra.ridge.RidgeMessage;

public class RidgeEnsembleNode {
   
   public static class PrintDeliverInterface implements DeliverInterface {

      @Override
      public void deliverConservatively(RidgeMessage message) {
         System.out.println(String.format("PrintDeliverInterface: Delivered message %s conservatively", message.getId()));
      }

      @Override
      public void deliverOptimistically(RidgeMessage message) {
         System.out.println(String.format("PrintDeliverInterface: Delivered message %s optmistically", message.getId()));         
      }

      @Override
      public void deliverFast(RidgeMessage message) {
         System.out.println(String.format("PrintDeliverInterface: Delivered message %s fast", message.getId()));
      }
      
   }
   
   ch.usi.dslab.bezerra.ridge.Process ridgeNode;
   int pid;
   
   public RidgeEnsembleNode(String configFile, int pid) {
      this.pid = pid;
      RidgeMulticastAgent.loadRidgeAgentConfig(configFile);
      ridgeNode = ch.usi.dslab.bezerra.ridge.Process.getProcess(pid);
   }
   
   public void startRunning() {
      if (ridgeNode instanceof Coordinator)
         System.out.println(String.format("Coordinator %d starting...", pid));
      else if (ridgeNode instanceof Learner)
         ((Learner) ridgeNode).setDeliverInterface(new PrintDeliverInterface());
      ridgeNode.startRunning();
   }
   
   public static void main(String[] args) {
      String configFile = args[0];
      int    pid        = Integer.parseInt(args[1]);
      RidgeEnsembleNode reNode = new RidgeEnsembleNode(configFile, pid);
      reNode.startRunning();
   }
}
