package ch.usi.dslab.bezerra.mcad.ridge;

import ch.usi.dslab.bezerra.ridge.Coordinator;

public class RidgeEnsembleNode {
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
      ridgeNode.startRunning();
   }
   
   public static void main(String[] args) {
      String configFile = args[0];
      int    pid        = Integer.parseInt(args[1]);
      RidgeEnsembleNode reNode = new RidgeEnsembleNode(configFile, pid);
      reNode.startRunning();
   }
}
