package ch.usi.dslab.bezerra.mcad.uringpaxos;

import ch.usi.da.paxos.api.LearnerCheckpoint;
import ch.usi.dslab.bezerra.mcad.MulticastCheckpoint;

public class URPMulticastCheckpoint implements MulticastCheckpoint {
   private static final long serialVersionUID = 655629245389078009L;
   
   LearnerCheckpoint learnerCheckpoint;
   
   public URPMulticastCheckpoint(LearnerCheckpoint lcp) {
      this.learnerCheckpoint = lcp;
   }

   public LearnerCheckpoint getLearnerCheckpoint() {
      return learnerCheckpoint;
   }

   public void setLearnerCheckpoint(LearnerCheckpoint learnerCheckpoint) {
      this.learnerCheckpoint = learnerCheckpoint;
   }
}
