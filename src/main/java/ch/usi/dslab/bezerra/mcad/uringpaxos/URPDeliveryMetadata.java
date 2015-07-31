package ch.usi.dslab.bezerra.mcad.uringpaxos;

import ch.usi.da.paxos.api.LearnerDeliveryMetadata;
import ch.usi.dslab.bezerra.mcad.DeliveryMetadata;

public class URPDeliveryMetadata extends DeliveryMetadata {
   private static final long serialVersionUID = 7355565506510035393L;

   LearnerDeliveryMetadata learnerDeliveryMetadata;


//   public URPDeliveryMetadata(int ringId, long instanceId) {
//      this.ringId     = ringId;
//      this.instanceId = instanceId;
//      this.batchPos   = -1;
//   }
   
   public LearnerDeliveryMetadata getLearnerDeliveryMetadata() {
      return learnerDeliveryMetadata;
   }

   public void setLearnerDeliveryMetadata(
         LearnerDeliveryMetadata learnerDeliveryMetadata) {
      this.learnerDeliveryMetadata = learnerDeliveryMetadata;
   }

   public URPDeliveryMetadata(LearnerDeliveryMetadata metadata) {
      this.learnerDeliveryMetadata = metadata;
   }
   
   @Override
   public boolean precedes(DeliveryMetadata o) {
      URPDeliveryMetadata other = (URPDeliveryMetadata) o;
      return this.learnerDeliveryMetadata.compareTo(other.learnerDeliveryMetadata) < 0;
   }

   @Override
   public int compareTo(DeliveryMetadata o) {
      URPDeliveryMetadata other = (URPDeliveryMetadata) o;
      return learnerDeliveryMetadata.compareTo(other.learnerDeliveryMetadata);      
   }

   @Override
   public boolean equals(Object o) {
      URPDeliveryMetadata other = (URPDeliveryMetadata) o;
      return this.learnerDeliveryMetadata.compareTo(other.learnerDeliveryMetadata) == 0;
   }

   @Override
   public int hashCode() {
      return learnerDeliveryMetadata.hashCode();
   }
   
   @Override
   public String toString() {
      return String.format("URPDeliveryMetadata: %s", learnerDeliveryMetadata);
   }

}
